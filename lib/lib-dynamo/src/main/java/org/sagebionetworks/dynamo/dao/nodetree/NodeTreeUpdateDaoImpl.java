package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.dynamo.DynamoWriteExecution;
import org.sagebionetworks.dynamo.DynamoWriteExecutor;
import org.sagebionetworks.dynamo.DynamoWriteOperation;
import org.sagebionetworks.dynamo.dao.DynamoDaoBaseImpl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

/**
 * Implementation based on ancestor-descendant and descendant-ancestor pointers.
 */
public class NodeTreeUpdateDaoImpl extends DynamoDaoBaseImpl implements NodeTreeUpdateDao {

	private final Logger logger = LogManager.getLogger(NodeTreeUpdateDaoImpl.class);

	private final DynamoDBMapper writeMapper;
	private final DynamoWriteExecutor writeExecutor;

	public NodeTreeUpdateDaoImpl(AmazonDynamoDB dynamoClient) {
		super(dynamoClient);

		DynamoDBMapperConfig mapperConfig = NodeLineageMapperConfig.getMapperConfigWithConsistentReads();
		this.writeMapper = new DynamoDBMapper(dynamoClient, mapperConfig);
		this.writeExecutor = new NodeLineageWriteExecutor();
	}

	@Override
	public boolean create(String child, String parent, Date timestamp) throws IncompletePathException {
		validateDynamoEnabled();
		return createOrUpdate(child, parent, timestamp);
	}

	@Override
	public boolean update(String child, String parent, Date timestamp)
			throws IncompletePathException, ObsoleteChangeException {
		validateDynamoEnabled();
		return createOrUpdate(child, parent, timestamp);
	}

	@Override
	public boolean delete(String nodeId, Date timestamp)
			throws ObsoleteChangeException {
		validateDynamoEnabled();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		if (timestamp == null) {
			throw new IllegalArgumentException("Timestamp cannot be null.");
		}

		logger.info("Deleting node " + nodeId + ".");

		// Create a new list that holds this node and its descendants
		// These nodes lineage with their ancestors will be cleared 
		List<NodeLineage> descList = NodeTreeDaoUtils.getDescendants(nodeId, writeMapper);
		final List<String> deleteList = new ArrayList<String>(descList.size() + 1);
		deleteList.add(nodeId);
		for (NodeLineage desc : descList) {
			deleteList.add(desc.getAncestorOrDescendantId());
		}

		List<DynamoWriteOperation> deleteOpList = new ArrayList<DynamoWriteOperation>();
		for (String deleteNode : deleteList) {
			// Note we do not check path-from-root for delete
			List<DboNodeLineage> dboList = NodeTreeDaoUtils.query(deleteNode, LineageType.ANCESTOR, -1, writeMapper);
			for (int i = 0; i < dboList.size(); i++) {
				DboNodeLineage dbo = dboList.get(i);
				if (timestamp.before(dbo.getTimestamp())) {
					throw new ObsoleteChangeException("Update message is obsolete.");
				}
				NodeLineagePair pair = new NodeLineagePair(dbo, i);
				LineagePairDelete delete = new LineagePairDelete(pair, writeMapper);
				deleteOpList.add(delete);
			}
		}

		String execId = createExecutionId(nodeId, null, timestamp, "delete");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, deleteOpList);
		boolean success = writeExecutor.execute(exec);
		if (!success) {
			logger.error("Deleting node " + nodeId + " failed.");
			return false;
		}
		logger.info("Deleting node " + nodeId + " succeeded.");
		return true;
	}

	// Private Methods ///////////////////////////////////////////////////////////

	private boolean createOrUpdate(String child, String parent, Date timestamp) {

		if (child == null || child.isEmpty()) {
			throw new IllegalArgumentException("Child node cannot be null or empty.");
		}
		if (parent == null || parent.isEmpty()) {
			throw new IllegalArgumentException("Parent node cannot be null or empty.");
		}
		if (timestamp == null) {
			throw new IllegalArgumentException("Timestamp cannot be null.");
		}

		final boolean asRoot = child.equals(parent);
		if (asRoot) {
			if (NodeTreeDaoUtils.isRoot(child, writeMapper)) {
				// Already a root in dynamo
				return true;
			}
		}

		// Should update, not create, if it already exists
		final boolean exists = NodeTreeDaoUtils.exists(child, writeMapper);
		if (exists) {
			return updatePair(child, parent, timestamp);
		}

		// If it does not exist, create
		if (asRoot) {
			return createRoot(child, timestamp);
		}

		return createPair(child, parent, timestamp);
	}

	/**
	 * Updates the child to make it point to the new parent. If the child
	 * is to be updated to be a root, make child point to itself (i.e. the child
	 * is passed in as the new parent).
	 */
	private boolean updatePair(final String child, final String parent, final Date timestamp) {

		// To update, make sure we are working with a newer change
		final NodeLineage parentLineage = NodeTreeDaoUtils.getParentLineage(child, writeMapper);
		if (timestamp.before(parentLineage.getTimestamp())) {
			throw new ObsoleteChangeException("Updating pair (child, parent) " + "(" + child + ", " + parent + ") is obsolete.");
		}

		// If the child is already pointing to the same parent, no update needed
		final String existingParent = parentLineage.getAncestorOrDescendantId();
		if (parent.equals(existingParent)) {
			return true;
		}

		final String msg = "Updating (child, parent): (" + child + ", " + parent + ")";
		logger.info(msg + ".");

		// To keep the update minimal, we find the part that actually changes.
		// For example, if we are updating node G such that
		// the path from the root to G, "/A/B/D/E/H/G", becomes "/A/B/D/F/G",
		// then "E/H" is the part whose pointers to the descendants need to be
		// deleted and "F" is the new part whose pointers to the descendants
		// need to be created. We only need to perform these deletes and creates.
		List<NodeLineage> curPath = NodeTreeDaoUtils.getCompletePathFromRoot(child, writeMapper);
		List<NodeLineage> newPath = new ArrayList<NodeLineage>(0);
		final boolean asRoot = child.equals(parent);
		if (!asRoot) {
			newPath = NodeTreeDaoUtils.getCompletePathFromRoot(parent, writeMapper);
		}
		int i = 0;
		final int curPathLength = curPath.size();
		final int newPathLength = newPath.size();
		while (i < curPathLength && i < newPathLength) {
			String nodeOnCurPath = curPath.get(i).getNodeId();
			String nodeOnNewPath = newPath.get(i).getNodeId();
			if (!nodeOnCurPath.equals(nodeOnNewPath)) {
				break;
			}
			i++;
		}
		final int start = i;

		// Create a new list that holds this node and its descendants
		// These nodes' lineage need to be updated with their ancestors
		List<NodeLineage> descList = NodeTreeDaoUtils.getDescendants(child, writeMapper);
		final List<String> updateList = new ArrayList<String>(descList.size() + 1);
		updateList.add(child);
		for (NodeLineage desc : descList) {
			updateList.add(desc.getAncestorOrDescendantId());
		}

		List<DynamoWriteOperation> deleteOpList = new ArrayList<DynamoWriteOperation>();
		List<DynamoWriteOperation> putOpList = new ArrayList<DynamoWriteOperation>();
		for (String updateNode : updateList) {
			// Since we will be creating delete ops, the node must already exist
			// on a complete path from the root
			List<NodeLineage> path = NodeTreeDaoUtils.getCompletePathFromRoot(updateNode, writeMapper);
			// Create the delete ops
			for (int u = start; u < curPathLength; u++) {
				NodeLineage lineage = path.get(u);
				int depth = u;
				NodeLineagePair pair = new NodeLineagePair(lineage, depth);
				LineagePairDelete delete = new LineagePairDelete(pair, writeMapper);
				deleteOpList.add(delete);
			}
			// Create the put ops
			for (int u = start; u < newPathLength; u++) {
				NodeLineage lineage = newPath.get(u);
				String ancestor = lineage.getAncestorOrDescendantId();
				int distance = path.size() - curPathLength + newPathLength + 1 - u;
				int depth = u;
				NodeLineagePair pair = new NodeLineagePair(ancestor, updateNode, depth, distance, timestamp);
				LineagePairPut put = new LineagePairPut(pair, writeMapper);
				putOpList.add(put);
			}
			// Do not forget the parent for the put op
			if (!asRoot) {
				int distance = path.size() - curPathLength + 1;
				int depth = newPathLength;
				NodeLineagePair pair = new NodeLineagePair(parent, updateNode, depth, distance, timestamp);
				LineagePairPut put = new LineagePairPut(pair, writeMapper);
				putOpList.add(put);
			}
		}

		// Handle root changes
		if (asRoot) {
			// In the case of promoting the child to be a root, we need to
			// add a pair of pointers between the child and dummy ROOT to make it a root
			final int distance = 1;
			final int depth = 0;
			NodeLineagePair rootPair = new NodeLineagePair(DboNodeLineage.ROOT, child,
					depth, distance, timestamp);
			LineagePairPut rootPut = new LineagePairPut(rootPair, writeMapper);
			putOpList.add(rootPut);
		} else if (DboNodeLineage.ROOT.equals(existingParent)) {
			// In the case the child already exists as a root, we need to
			// remove the pair of pointers between the child and dummy ROOT.
			final int depth = 0;
			NodeLineagePair rootPair = new NodeLineagePair(parentLineage, depth);
			LineagePairDelete rootDelete = new LineagePairDelete(rootPair, writeMapper);
			deleteOpList.add(rootDelete);
		}

		logger.info(msg + " -- Removing old pointers.");

		// Execute the deletes first
		String execId = createExecutionId(child, parent, timestamp, "update-pair-delete");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, deleteOpList);
		boolean success = writeExecutor.execute(exec);
		if (!success) {
			logger.error(msg + " -- Removing old pointers failed.");
			return false;
		}

		logger.info(msg + " -- Adding new pointers.");

		// Then the puts
		execId = createExecutionId(child, parent, timestamp, "update-pair-put");
		exec = new DynamoWriteExecution(execId, putOpList);

		success = writeExecutor.execute(exec);
		if (!success) {
			logger.error(msg + " -- Adding new pointers failed.");
			return false;
		}
		logger.info(msg + " succeeded.");
		return true;
	}

	/**
	 * Creates a new root.
	 */
	private boolean createRoot(final String nodeId, final Date timestamp) {

		logger.info("Creating root " + nodeId + ".");

		NodeLineagePair pair = new NodeLineagePair(DboNodeLineage.ROOT, nodeId, 0, 1, timestamp);
		LineagePairPut put = new LineagePairPut(pair, writeMapper);
		String execId = createExecutionId(nodeId, nodeId, timestamp, "create-root");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, put);

		boolean success = writeExecutor.execute(exec);
		if (!success) {
			logger.error("Creating root " + nodeId + " failed.");
			return false;
		}
		logger.info("Root " + nodeId + " created.");
		return true;
	}

	/**
	 * Adds a new (child, parent) pair.
	 */
	private boolean createPair(final String child, final String parent, final Date timestamp) {

		String msg = "Creating (child, parent): (" + child + ", " + parent + ")";
		logger.info(msg + ".");

		// Remove previously unfinished updates if there are any
		// (e.g. unfinished deletes due to throttling)
		delete(child, timestamp);

		// The parent must already exist on a complete path all the way from the root
		// If the parent does not already exist, we should get IncompletePathException
		// Note the list can be empty where the parent is the root
		List<NodeLineage> rootToParent = NodeTreeDaoUtils.getCompletePathFromRoot(
				parent, writeMapper);

		// Go through the ancestors to create the put pairs
		List<DynamoWriteOperation> putList = new ArrayList<DynamoWriteOperation>();
		int depth = 0;
		int distance = rootToParent.size() + 1;
		for (NodeLineage lineage : rootToParent) {
			String ancestor = lineage.getAncestorOrDescendantId();
			NodeLineagePair pair = new NodeLineagePair(ancestor, child, depth, distance, timestamp);
			LineagePairPut put = new LineagePairPut(pair, writeMapper);
			putList.add(put);
			distance--;
			depth++;
		}
		// Do not forget the parent
		NodeLineagePair pair = new NodeLineagePair(parent, child, depth, distance, timestamp);
		LineagePairPut put = new LineagePairPut(pair, writeMapper);
		putList.add(put);

		String execId = createExecutionId(child, parent, timestamp, "create");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, putList);

		boolean success = writeExecutor.execute(exec);
		if (!success) {
			logger.error(msg + " failed.");
			return false;
		}
		logger.info(msg + " succeeded.");
		return true;
	}

	/**
	 * Creates an execution ID (name) from the (child, parent) tuple.
	 */
	private String createExecutionId(final String child, final String parent,
			final Date timestamp, final String op) {
		return "Execution [child=" + child + ", parent=" + parent +
				", timestamp=" + timestamp + ", operation=" + op + "]";
	}
}
