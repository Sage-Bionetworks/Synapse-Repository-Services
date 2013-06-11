package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;

/**
 * Utility class that provides raw operations on node lineage trees in DynamoDB.
 */
class NodeTreeDaoUtils {

	/** The maximum number of items to retrieve per page. */
	private static final int MAX_PAGE_SIZE = 100000;

	/**
	 * Gets the path from the root (inclusive) to this node (exclusive). The path must be complete.
	 * That is to say the depth must start with depth 0 (the root) and must be consecutive all the
	 * way to the depth of this node. The dummy root is NOT included in the list. An empty list is
	 * returned if this node is the actual root.
	 *
	 * @throws IncompletePathException When the path is incomplete
	 */
	static List<NodeLineage> getCompletePathFromRoot(final String node, final DynamoDBMapper mapper)
			throws IncompletePathException {

		// The list of ancestors in the order of distance from this node. The list includes
		// the dummy root if and only if this node is the actual root.
		final List<DboNodeLineage> dboList = query(node, LineageType.ANCESTOR, -1, mapper);

		if (dboList == null) {
			throw new NoAncestorException("Null list of ancestors returned from DynamoDB for node " + node);
		}

		final int dboListSize = dboList.size();
		if (dboListSize == 0) {
			// We should at least have the actual root or the dummy ROOT.
			throw new NoAncestorException("Empty list of ancestors returned from DynamoDB for node " + node);
		}

		// Start from checking the root
		final NodeLineage lastLineage = new NodeLineage(dboList.get(dboListSize - 1));
		final String lastNode = lastLineage.getAncestorOrDescendantId();
		// In the case the querying node is the actually root, the last node would be the dummy ROOT
		if (DboNodeLineage.ROOT.equals(lastNode) && isRoot(node, mapper)) {
			return new ArrayList<NodeLineage>(0);
		}
		if (!isRoot(lastNode, mapper)) {
			throw new IncompletePathException("The list of ancestors for node " + node + " is missing the root.");
		}

		// Check the rest one by one and make sure we get all the nodes on the path
		final List<NodeLineage> path = new ArrayList<NodeLineage>(dboListSize);
		for (int i = dboListSize - 1; i >= 0; i--) {
			DboNodeLineage dbo = dboList.get(i);
			NodeLineage lineage = new NodeLineage(dbo);
			int dist = lineage.getDistance();
			// The list is sorted by distance. The first ancestor at index 0 is the parent
			// whose distance is 1. If we have a complete path, the distance of any ancestor
			// in the list should be exactly i+1.
			if (dist < (i + 1)) {
				throw new IncompletePathException("Missing ancestor at distance " + (i + 1)
						+ " in the ancestor path for node " + node);
			} else if (dist > (i + 1)) {
				String msg = "Node " + node + " has more than 1 ancestor at distance " + dist;
				List<NodeLineage> ancestors = new ArrayList<NodeLineage>();
				ancestors.add(lineage);
				// And also the previous one
				if ((i + 1) < dboListSize) {
					ancestors.add(new NodeLineage(dboList.get(i + 1)));
				}
				throw new MultipleInheritanceException(msg, node, dist, ancestors);
			}
			path.add(lineage);
		}

		return path;
	}

	/**
	 * Whether the node is a root.
	 */
	static boolean isRoot(final String nodeId, final DynamoDBMapper mapper) {
		NodeLineage parent = getParentLineage(nodeId, mapper);
		return parent != null &&
				DboNodeLineage.ROOT.equals(parent.getAncestorOrDescendantId());
	}

	/**
	 * Whether the node exists in dynamo.
	 */
	static boolean exists(final String nodeId, final DynamoDBMapper mapper) {
		NodeLineage parentLineage = getParentLineage(nodeId, mapper);
		return (parentLineage != null);
	}

	/**
	 * Gets the lineage to the parent node. Returns null if the child does not exist yet
	 * in DynamoDB. The root will return the dummy ROOT as the parent.
	 */
	static NodeLineage getParentLineage(final String child, final DynamoDBMapper mapper) {
		// Parent is exactly 1 node away
		List<DboNodeLineage> dboList = query(child, LineageType.ANCESTOR, 1, mapper);
		if (dboList == null || dboList.isEmpty()) {
			return null;
		}
		if (dboList.size() > 1) {
			String msg = child + " fetches back more than 1 parent.";
			List<NodeLineage> parents = new ArrayList<NodeLineage>(dboList.size());
			for (DboNodeLineage dbo : dboList) {
				parents.add(new NodeLineage(dbo));
			}
			throw new MultipleParentsException(msg, child, parents);
		}
		NodeLineage parent = new NodeLineage(dboList.get(0));
		return parent;
	}

	/**
	 * Finds all the descendants. Use this internally for updating only.
	 */
	static List<NodeLineage> getDescendants(final String nodeId, final DynamoDBMapper mapper) {
		List<DboNodeLineage> dboList = query(nodeId, LineageType.DESCENDANT, -1, mapper);
		final List<NodeLineage> descList = new ArrayList<NodeLineage>(dboList.size());
		int count = 0;
		for (DboNodeLineage dbo : dboList) {
			count++;
			// The query method handles paging internally. It returns a 'lazy-loaded' collection.
			// That is, it initially returns only one page of results, and then makes a service
			// call for the next page if needed. Iterating over the list fetches the whole
			// collection page by page.
			descList.add(new NodeLineage(dbo));
			if (count > MAX_PAGE_SIZE) {
				// Are we querying the root for its descendants?
				throw new RuntimeException("Max page size, " + MAX_PAGE_SIZE
						+ ", reached for the descendants of node " + nodeId);
			}
		}
		return descList;
	}

	/**
	 * Performs a simple query on the specified node.
	 *
	 * @param nodeId        The node to perform query on
	 * @param distance      The distance to the node, the range key, supply a value <= 0 to ignore the range
	 * @param lineageType   The type of query, whether it is ancestor or descendant
	 * @param dynamoMapper  The DynamoDB mapper used to perform the query
	 */
	static List<DboNodeLineage> query(final String nodeId, final LineageType lineageType,
			final int distance, final DynamoDBMapper dynamoMapper) {

		final String hashKey = DboNodeLineage.createHashKey(nodeId, lineageType);
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression(hashKeyAttr);

		if (distance > 0) {
			String rangeKeyStart = DboNodeLineage.createRangeKey(distance, "");
			Condition rangeKeyCondition = new Condition()
					.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
					.withAttributeValueList(new AttributeValue().withS(rangeKeyStart));
			queryExpression.setRangeKeyCondition(rangeKeyCondition);
		}

		List<DboNodeLineage> dboList = dynamoMapper.query(DboNodeLineage.class, queryExpression);
		return dboList;
	}
}
