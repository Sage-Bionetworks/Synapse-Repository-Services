package org.sagebionetworks.dynamo.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.sagebionetworks.dynamo.DynamoWriteExecution;
import org.sagebionetworks.dynamo.DynamoWriteExecutor;
import org.sagebionetworks.dynamo.DynamoWriteOperation;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.Key;

/**
 * Implements using the node lineage model.
 *
 * @author Eric Wu
 */
public class NodeTreeDaoNodeLineageImpl implements NodeTreeDao {

	private final Logger logger = Logger.getLogger(NodeTreeDaoNodeLineageImpl.class);

	@Autowired
	private AmazonDynamoDB dynamoClient;
	private final DynamoDBMapper writeMapper;
	private final DynamoDBMapper readMapper;
	private final DynamoWriteExecutor writeExecutor;

	public NodeTreeDaoNodeLineageImpl() {
		this.writeMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfigWithConsistentReads());
		this.readMapper = new DynamoDBMapper(this.dynamoClient,
				NodeLineageMapperConfig.getMapperConfig());
		this.writeExecutor = new NodeLineageWriteExecutor();
	}

	@Override
	public boolean create(final String child, final String parent, final Date timestamp)
			throws ParentNotFoundException, IncompletePathException {

		if (child == null) {
			throw new NullPointerException();
		}
		if (parent == null) {
			throw new NullPointerException();
		}
		if (timestamp == null) {
			throw new NullPointerException();
		}

		// The root node
		if (child.equals(parent)) {
			NodeLineage rootLineage = this.getRootLineage(this.writeMapper);
			if (rootLineage != null) {
				String root = rootLineage.getAncestorOrDescendantId();
				if (child.equals(root)) {
					return true;
				}
				String msg = "The root already exists";
				msg = msg + " but on a different node. The root is ";
				msg = msg + root + ". The change node is " + child;
				throw new RuntimeException(msg);
			}
			NodeLineagePair pair = new NodeLineagePair(DboNodeLineage.ROOT, child, 0, 1, timestamp);
			LineagePairPut put = new LineagePairPut(pair, this.writeMapper);
			String execId = this.createExecutionId(child, parent, timestamp, "create");
			DynamoWriteExecution exec = new DynamoWriteExecution(execId, put);
			return this.writeExecutor.execute(exec);
		}

		// Check if this child already exists
		NodeLineage parentLineage = this.getParentLineage(child, this.writeMapper);
		if (parentLineage != null) {
			String parentId = parentLineage.getAncestorOrDescendantId();
			if (parent.equals(parentId)) {
				// The same (child, parent) already exists
				// We will skip the create and keep the old timestamp
				return true;
			} else {
				// If the child already exists but points to a different parent
				return this.update(child, parent, timestamp);
			}
		}

		// The parent must already exist on a complete path all the way from the root
		// If the parent does not already exist, we should get IncompletePathException
		// Note the list can be empty where parent is the root
		List<NodeLineage> rootToParent = this.getCompletePathFromRoot(parent, this.writeMapper);

		// Create all the put pairs
		List<DynamoWriteOperation> putList = new ArrayList<DynamoWriteOperation>();
		// Go through the ancestors
		int depth = 0;
		int distance = rootToParent.size() + 1;
		for (NodeLineage lineage : rootToParent) {
			String ancestor = lineage.getAncestorOrDescendantId();
			NodeLineagePair pair = new NodeLineagePair(ancestor, child, depth, distance, timestamp);
			LineagePairPut put = new LineagePairPut(pair, this.writeMapper);
			putList.add(put);
			distance--;
			depth++;
		}
		// Do not forget the parent
		NodeLineagePair pair = new NodeLineagePair(parent, child, depth, distance, timestamp);
		LineagePairPut put = new LineagePairPut(pair, this.writeMapper);
		putList.add(put);

		String execId = this.createExecutionId(child, parent, timestamp, "create");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, putList);
		return this.writeExecutor.execute(exec);
	}

	@Override
	public boolean update(String child, String parent, Date timestamp) {

		if (child == null) {
			throw new NullPointerException();
		}
		if (parent == null) {
			throw new NullPointerException();
		}
		if (timestamp == null) {
			throw new NullPointerException();
		}

		// The root node
		if (child.equals(parent)) {
			NodeLineage rootLineage = this.getRootLineage(this.writeMapper);
			if (rootLineage != null) {
				String root = rootLineage.getAncestorOrDescendantId();
				if (child.equals(root)) {
					return true;
				}
				String msg = "The root already exists";
				msg = msg + " but on a different node. The root is ";
				msg = msg + root + ". The change node is " + child;
				throw new RuntimeException(msg);
			}
		}

		// If the child is currently pointing to a parent
		NodeLineage parentLineage = this.getParentLineage(child, this.writeMapper);
		if (parentLineage == null) {
			// If not, we should create
			return this.create(child, parent, timestamp);
		}

		// If the child is already pointing to the same parent, no update needed
		String parentId = parentLineage.getAncestorOrDescendantId();
		if (parent.equals(parentId)) {
			// The same (child, parent) already exists
			// We will skip the create and keep the old timestamp
			return true;
		}

		// To update, make sure we are working with a newer message 
		if (timestamp.before(parentLineage.getTimestamp())) {
			throw new RuntimeException("Update message is obsolete.");
		}

		// To keep the update minimal, we find the part that actually changes
		// For example, if we are updating node G such that
		// the path from the root to G, "/A/B/D/E/H/G", becomes "/A/B/D/F/G",
		// then "E/H" is the part whose pointers to the descendants need to be
		// deleted and "F" is the new part whose pointers to the descendants
		// need to be created.
		List<NodeLineage> curPath = this.getCompletePathFromRoot(child, this.writeMapper);
		List<NodeLineage> newPath = this.getCompletePathFromRoot(parent, this.writeMapper);
		int i = 0;
		final int curPathLength = curPath.size();
		final int newPathLength = newPath.size();
		while (i < curPathLength && i < newPathLength) {
			NodeLineage nodeOnCurPath = curPath.get(i);
			NodeLineage nodeOnNewPath = newPath.get(i);
			if (!nodeOnCurPath.getAncestorOrDescendantId().equals(
					nodeOnNewPath.getAncestorOrDescendantId())) {
				break;
			}
			i++;
		}
		final int start = i;

		// Create a new list that holds this node and its descendants
		// These nodes' lineage need to be updated with their ancestors
		List<NodeLineage> descList = this.getDescendants(child, this.writeMapper);
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
			List<NodeLineage> path = this.getCompletePathFromRoot(updateNode, this.writeMapper);
			// Create the delete ops
			for (int u = start; u < curPathLength; u++) {
				NodeLineage lineage = path.get(u);
				int depth = u;
				NodeLineagePair pair = new NodeLineagePair(lineage, depth);
				LineagePairDelete delete = new LineagePairDelete(pair, this.writeMapper);
				deleteOpList.add(delete);
			}
			// Create the put ops
			for (int u = start; u < newPath.size(); u++) {
				NodeLineage lineage = newPath.get(u);
				String ancestor = lineage.getAncestorOrDescendantId();
				int distance = path.size() - curPathLength + newPathLength + 1 - u;
				int depth = u;
				NodeLineagePair pair = new NodeLineagePair(ancestor, updateNode, depth, distance, timestamp);
				LineagePairPut put = new LineagePairPut(pair, this.writeMapper);
				putOpList.add(put);
			}
			// Do not forget the parent for the put op
			int distance = path.size() - curPathLength + 1;
			int depth = newPathLength;
			NodeLineagePair pair = new NodeLineagePair(parent, updateNode, depth, distance, timestamp);
			LineagePairPut put = new LineagePairPut(pair, this.writeMapper);
			putOpList.add(put);
		}

		// Execute the deletes first
		String execId = this.createExecutionId(child, parent, timestamp, "update-delete");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, deleteOpList);
		boolean success = this.writeExecutor.execute(exec);
		if (!success) {
			return false;
		}

		// Then the puts
		execId = this.createExecutionId(child, parent, timestamp, "update-put");
		exec = new DynamoWriteExecution(execId, putOpList);
		return this.writeExecutor.execute(exec);
	}

	@Override
	public boolean delete(String nodeId, Date timestamp) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (timestamp == null) {
			throw new NullPointerException();
		}

		// Create a new list that holds this node and its descendants
		// These nodes' lineage need to be updated with their ancestors
		List<NodeLineage> descList = this.getDescendants(nodeId, this.writeMapper);
		final List<String> updateList = new ArrayList<String>(descList.size() + 1);
		updateList.add(nodeId);
		for (NodeLineage desc : descList) {
			updateList.add(desc.getAncestorOrDescendantId());
		}

		List<DynamoWriteOperation> deleteOpList = new ArrayList<DynamoWriteOperation>();
		for (String updateNode : updateList) {
			// Note we do not check path-from-root for delete
			String hashKey = DboNodeLineage.createHashKey(updateNode, LineageType.ANCESTOR);
			AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
			DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
			List<DboNodeLineage> dboList = this.writeMapper.query(DboNodeLineage.class, queryExpression);
			for (int i = 0; i < dboList.size(); i++) {
				DboNodeLineage dbo = dboList.get(i);
				if (timestamp.before(dbo.getTimestamp())) {
					throw new RuntimeException("Update message is obsolete.");
				}
				NodeLineagePair pair = new NodeLineagePair(dbo, i);
				LineagePairDelete delete = new LineagePairDelete(pair, this.writeMapper);
				deleteOpList.add(delete);
			}
		}

		String execId = this.createExecutionId(nodeId, null, timestamp, "delete");
		DynamoWriteExecution exec = new DynamoWriteExecution(execId, deleteOpList);
		return this.writeExecutor.execute(exec);
	}

	@Override
	public List<String> getAncestors(String nodeId) throws IncompletePathException {

		if (nodeId == null) {
			throw new NullPointerException();
		}

		List<NodeLineage> path = this.getCompletePathFromRoot(nodeId, this.readMapper);
		List<String> ancestorList = new ArrayList<String>(path.size());
		for (int i = 0; i < path.size(); i++) {
			ancestorList.add(path.get(i).getAncestorOrDescendantId());
		}
		return Collections.unmodifiableList(ancestorList);
	}

	@Override
	public String getParent(String nodeId) {

		if (nodeId == null) {
			throw new NullPointerException();
		}

		NodeLineage parentLineage = this.getParentLineage(nodeId, this.readMapper);
		if (parentLineage != null) {
			return parentLineage.getAncestorOrDescendantId();
		}
		return null;
	}

	@Override
	public List<String> getDescendants(String nodeId, int pageSize, String lastDescIdExcl) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (pageSize < 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Key lastKeyEvaluated = this.createKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = this.getDescendants(nodeId, this.readMapper, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	@Override
	public List<String> getDescendants(String nodeId, int generation, int pageSize, String lastDescIdExcl) {

		if (nodeId == null) {
			throw new NullPointerException();
		}
		if (generation < 1) {
			throw new IllegalArgumentException("Must be at least 1 generation away.");
		}
		if (pageSize < 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Key lastKeyEvaluated = this.createKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = this.getDescendants(nodeId, this.readMapper, generation, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	@Override
	public List<String> getChildren(String nodeId, int pageSize, String lastDescIdExcl) {
		// Children are exactly one generation away
		return this.getDescendants(nodeId, 1, pageSize, lastDescIdExcl);
	}

	@Override
	public List<String> getPath(String nodeX, String nodeY)
			throws IncompletePathException {

		if (nodeX == null) {
			throw new NullPointerException();
		}
		if (nodeY == null) {
			throw new NullPointerException();
		}
		if (nodeX.equals(nodeY)) {
			return new ArrayList<String>(0);
		}

		List<NodeLineage> pathX = this.getCompletePathFromRoot(nodeX, this.readMapper);
		List<NodeLineage> pathY = this.getCompletePathFromRoot(nodeY, this.readMapper);

		// X and Y are not on the same lineage if their depth are the same
		int depthX = pathX.size();
		int depthY = pathY.size();
		if (depthX == depthY) {
			return null;
		}
		// Which is deeper?
		// If X and Y are on the same lineage, the deeper node is the descendant
		// We walk the deeper path to find the ancestor
		List<NodeLineage> path = depthX > depthY ? pathX : pathY;
		String descendant = depthX > depthY ? nodeX : nodeY;
		String ancestor = depthX > depthY ? nodeY : nodeX;

		List<NodeLineage> pathInBetween = null;
		for (int i = 0; i < path.size(); i++) {
			NodeLineage lineage = path.get(i);
			if (lineage.getAncestorOrDescendantId().equals(ancestor)) {
				pathInBetween = new ArrayList<NodeLineage>(path.subList(i, path.size()));
				break;
			}
		}

		if (pathInBetween == null) {
			return null;
		}

		List<String> results = new ArrayList<String>(pathInBetween.size() + 1);
		for (int i = 0; i < pathInBetween.size(); i++) {
			results.add(pathInBetween.get(i).getAncestorOrDescendantId());
		}
		results.add(descendant);
		return results;
	}

	@Override
	public String getLowestCommonAncestor(String nodeX, String nodeY)
			throws IncompletePathException {

		if (nodeX == null) {
			throw new NullPointerException();
		}
		if (nodeY == null) {
			throw new NullPointerException();
		}

		List<NodeLineage> pathX = this.getCompletePathFromRoot(nodeX, this.readMapper);
		List<NodeLineage> pathY = this.getCompletePathFromRoot(nodeY, this.readMapper);
		String node = null;
		for (int i = 0; i < pathX.size() && i < pathY.size(); i++) {
			String nX = pathX.get(i).getAncestorOrDescendantId();
			String nY = pathY.get(i).getAncestorOrDescendantId();
			if (!nX.equals(nY)) {
				break;
			}
			node = nX;
		}
		return node;
	}

	/////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an execution ID (name) from the (child, parent) tuple.
	 */
	private String createExecutionId(final String child, final String parent,
			final Date timestamp, final String op) {
		return "Execution [child=" + child + ", parent=" + parent +
				", timestamp=" + timestamp + ", operation=" + op + "]";
	}

	/**
	 * Gets the lineage to the root node. Null if the root does not exist yet.
	 * This is essentially the pointer from the dummy ROOT to the actual root.
	 */
	private NodeLineage getRootLineage(DynamoDBMapper mapper) {

		assert mapper != null;

		// Use the dummy ROOT to locate the actual root
		String hashKey = DboNodeLineage.ROOT_HASH_KEY;
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);

		String rangeKey = DboNodeLineage.createRangeKey(1, "");
		Condition rangeKeyCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
				.withAttributeValueList(new AttributeValue().withS(rangeKey));
		queryExpression.setRangeKeyCondition(rangeKeyCondition);

		List<DboNodeLineage> results = mapper.query(DboNodeLineage.class, queryExpression);

		if (results == null || results.isEmpty()) {
			// Log as info. This should not happen often.
			this.logger.info("Root not found.");
			return null;
		}
		if (results.size() > 1) {
			throw new RuntimeException("Dummy ROOT fetches back more than 1 actual root.");
		}

		NodeLineage lineage = new NodeLineage(results.get(0));
		return lineage;
	}

	/**
	 * Gets the lineage to the parent node. Returns null if the child does not exist yet
	 * in DynamoDB. The parent may or may not exist. The root will return the dummy ROOT
	 * as the parent.
	 */
	private NodeLineage getParentLineage(String child, DynamoDBMapper mapper) {

		assert child != null;
		assert mapper != null;

		String hashKey = DboNodeLineage.createHashKey(child, LineageType.ANCESTOR);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);

		// Parent is exactly 1 node away
		String rangeKeyStart = DboNodeLineage.createRangeKey(1, "");
		Condition rangeKeyCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
				.withAttributeValueList(new AttributeValue().withS(rangeKeyStart));
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);
		queryExpression.setRangeKeyCondition(rangeKeyCondition);

		List<DboNodeLineage> dboList = mapper.query(DboNodeLineage.class, queryExpression);

		if (dboList == null || dboList.isEmpty()) {
			return null;
		}
		if (dboList.size() > 1) {
			throw new RuntimeException(child + " fetch back more than 1 parent.");
		}

		NodeLineage parent = new NodeLineage(dboList.get(0));
		return parent;
	}

	/**
	 * Gets the path from the root (inclusive) to this node (exclusive). The path must be complete.
	 * That is to say the depth must start with depth 0 (the root) and must be consecutive all the
	 * way to the depth of this node. The dummy root is NOT included in the list. An empty list is
	 * returned if this node is the actual root.
	 *
	 * @throws IncompletePathException When the path is incomplete
	 */
	private List<NodeLineage> getCompletePathFromRoot(String node, DynamoDBMapper mapper)
			throws IncompletePathException {

		assert node != null;
		assert mapper != null;

		String hashKey = DboNodeLineage.createHashKey(node, LineageType.ANCESTOR);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);

		// The list of ancestors in the order of distance from this node. The list includes
		// the dummy root if and only if this node is the actual root.
		final List<DboNodeLineage> dboList = mapper.query(DboNodeLineage.class, queryExpression);

		if (dboList == null) {
			throw new IncompletePathException("Null list of ancestors returned from DynamoDB for node " + node);
		}

		final int dboListSize = dboList.size();
		if (dboListSize == 0) {
			// We should at least have the actual root or the dummy ROOT.
			throw new IncompletePathException("Empty list of ancestors returned from DynamoDB for node " + node);
		}

		// Start checking from the root
		final NodeLineage rootLineage = this.getRootLineage(mapper);
		if (rootLineage == null) {
			// The root must exist
			throw new IncompletePathException("The root does not exist yet in DynamodB.");
		}

		// Check if the last is the root. The last is the either the dummy root or the actual root.
		final NodeLineage lastLineage = new NodeLineage(dboList.get(dboListSize - 1));
		final String lastNode = lastLineage.getAncestorOrDescendantId();
		final String root = rootLineage.getAncestorOrDescendantId();
		if (!DboNodeLineage.ROOT.equals(lastNode) && !root.equals(lastNode)) {
			throw new IncompletePathException("The list of ancestors for node " + node + " is missing the root.");
		}

		// If the last node is the dummy node, this node must be the actual root.
		// No other node points to the dummy node except the actual root.
		if (DboNodeLineage.ROOT.equals(lastNode)) {
			if (!root.equals(node)) {
				throw new IncompletePathException("Node " + node + " is not the root but points to the dummy root.");
			}
			return new ArrayList<NodeLineage>(0);
		}

		// Check the rest one by one and make sure we get all the nodes on the path
		final List<NodeLineage> path = new ArrayList<NodeLineage>(dboListSize);
		for (int i = dboListSize - 1; i >= 0; i--) {
			DboNodeLineage dbo = dboList.get(i);
			NodeLineage lineage = new NodeLineage(dbo);
			int dist = lineage.getDistance();
			if (dist != (i + 1)) {
				throw new IncompletePathException("Missing node at depth " + (dboListSize - i - 1) + " on the path.");
			}
			path.add(lineage);
		}

		return path;
	}

	/**
	 * Finds all the descendants. Use this internally for updating only.
	 */
	private List<NodeLineage> getDescendants(String nodeId, DynamoDBMapper mapper) {

		assert nodeId != null;
		assert mapper != null;

		List<NodeLineage> result = this.getDescendants(nodeId, mapper, NodeTreeDao.MAX_PAGE_SIZE, null);
		if (result.size() == NodeTreeDao.MAX_PAGE_SIZE) {
			// Hints there are more pages. Are we updating the root?
			logger.error("Max page size arrived for the descendants of node " + nodeId);
		}
		return result;
	}

	private List<NodeLineage> getDescendants(String nodeId, DynamoDBMapper mapper, int pageSize, Key lastKeyEvaluated) {

		assert nodeId != null;
		assert mapper != null;
		assert pageSize > 0;

		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr)
				.withLimit(pageSize)
				.withExclusiveStartKey(lastKeyEvaluated);
		PaginatedQueryList<DboNodeLineage> result = mapper.query(DboNodeLineage.class, queryExpression);
		final List<NodeLineage> descList = new ArrayList<NodeLineage>(result.size());
		for (DboNodeLineage dbo : result) {
			descList.add(new NodeLineage(dbo));
		}
		return descList;
	}

	private List<NodeLineage> getDescendants(String nodeId, DynamoDBMapper mapper, int distance, int pageSize, Key lastKeyEvaluated) {

		assert nodeId != null;
		assert mapper != null;
		assert distance > 0;
		assert pageSize > 0;

		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		String rangeKeyStart = DboNodeLineage.createRangeKey(distance, "");
		Condition rangeKeyCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
				.withAttributeValueList(new AttributeValue().withS(rangeKeyStart));
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr)
				.withRangeKeyCondition(rangeKeyCondition)
				.withLimit(pageSize)
				.withExclusiveStartKey(lastKeyEvaluated);
		PaginatedQueryList<DboNodeLineage> result = mapper.query(DboNodeLineage.class, queryExpression);
		final List<NodeLineage> descList = new ArrayList<NodeLineage>(result.size());
		for (DboNodeLineage dbo : result) {
			descList.add(new NodeLineage(dbo));
		}
		return descList;
	}

	/**
	 * Creates the key of the last item to get the next page.
	 */
	private Key createKey(String nodeId, String descId) throws IncompletePathException {

		if (descId == null) {
			return null; // Null key fetches first page
		}

		assert nodeId != null;

		List<String> path = this.getPath(nodeId, descId);
		if (path == null) {
			throw new IncompletePathException("Path does not exist between nodes " + nodeId + " and " + descId);
		}

		int distance = path.size();
		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		AttributeValue hashKeyValue = new AttributeValue().withS(hashKey);
		String rangeKey = DboNodeLineage.createRangeKey(distance, descId);
		AttributeValue rangeKeyValue = new AttributeValue().withS(rangeKey);
		Key lastKeyEvaluated = new Key().withHashKeyElement(hashKeyValue).withRangeKeyElement(rangeKeyValue);
		return lastKeyEvaluated;
	}
}
