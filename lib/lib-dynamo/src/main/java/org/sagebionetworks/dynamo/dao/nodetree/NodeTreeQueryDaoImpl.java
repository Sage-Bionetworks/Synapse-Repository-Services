package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;

/**
 * Implementation based on ancestor-descendant and descendant-ancestor pointers.
 */
public class NodeTreeQueryDaoImpl implements NodeTreeQueryDao {

	private final AmazonDynamoDB dynamoClient;
	private final DynamoDBMapper readMapper;

	public NodeTreeQueryDaoImpl(AmazonDynamoDB dynamoClient) {

		if (dynamoClient == null) {
			throw new IllegalArgumentException("DynamoDB client cannot be null.");
		}

		this.dynamoClient = dynamoClient;
		DynamoDBMapperConfig mapperConfig = NodeLineageMapperConfig.getMapperConfig();
		this.readMapper = new DynamoDBMapper(this.dynamoClient, mapperConfig);
	}

	@Override
	public boolean isRoot(String nodeId) {
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		return NodeTreeDaoUtils.isRoot(nodeId, readMapper);
	}

	@Override
	public List<String> getAncestors(String nodeId) throws IncompletePathException {

		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}

		try {
			List<NodeLineage> path = NodeTreeDaoUtils.getCompletePathFromRoot(nodeId, readMapper);
			List<String> ancestorList = new ArrayList<String>(path.size());
			for (int i = 0; i < path.size(); i++) {
				ancestorList.add(path.get(i).getAncestorOrDescendantId());
			}
			return Collections.unmodifiableList(ancestorList);
		} catch (NoAncestorException e) {
			return Collections.unmodifiableList(new ArrayList<String>(0));
		}
	}

	@Override
	public String getParent(String nodeId) {

		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}

		NodeLineage parentLineage = NodeTreeDaoUtils.getParentLineage(nodeId, readMapper);
		if (parentLineage != null) {
			return parentLineage.getAncestorOrDescendantId();
		}
		return null;
	}

	@Override
	public List<String> getDescendants(String nodeId, int pageSize, String lastDescIdExcl) {

		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Key lastKeyEvaluated = createPagingKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = getDescendants(nodeId, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	@Override
	public List<String> getDescendants(String nodeId, int generation, int pageSize, String lastDescIdExcl) {

		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		if (generation < 1) {
			throw new IllegalArgumentException("Must be at least 1 generation away.");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Key lastKeyEvaluated = createPagingKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = getDescendants(nodeId, generation, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	// Private Methods ////////////////////////////////////////////////////////////////////////////

	private List<NodeLineage> getDescendants(String nodeId, int pageSize, Key lastKeyEvaluated) {
		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		return getDescendants(hashKey, null, pageSize, lastKeyEvaluated, false);
	}

	private List<NodeLineage> getDescendants(String nodeId, int distance,
			int pageSize, Key lastKeyEvaluated) {
		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		String rangeKeyStart = DboNodeLineage.createRangeKey(distance, "");
		Condition rangeKeyCondition = new Condition()
				.withComparisonOperator(ComparisonOperator.BEGINS_WITH)
				.withAttributeValueList(new AttributeValue().withS(rangeKeyStart));
		return getDescendants(hashKey, rangeKeyCondition, pageSize, lastKeyEvaluated, false);
	}

	/*
	 * Note that the following is ignored by DynamoDBMapper.query() and thus has no effect:
	 *
	 * DynamoDBQueryExpression
	 *         .withLimit(pageSize)
	 *         .withExclusiveStartKey(lastKeyEvaluated);
	 *
	 * DynamoDBMapper.query() implements lazy-load instead.
	 *
	 * Thus we are using the low-level SDK to do the paging.
	 */
	private List<NodeLineage> getDescendants(String hashKey, Condition rangeKeyCondition,
			int pageSize, Key lastKeyEvaluated, boolean consistentRead) {

		if (pageSize > 1000) {
			pageSize = 1000;
		}

		String tableName = NodeLineageMapperConfig
				.getMapperConfig()
				.getTableNameOverride()
				.getTableName();
		AttributeValue hashKeyAttr = new AttributeValue().withS(hashKey);
		QueryRequest queryRequest = new QueryRequest()
				.withTableName(tableName)
				.withHashKeyValue(hashKeyAttr)
				.withLimit(pageSize)
				.withConsistentRead(consistentRead);

		if (rangeKeyCondition != null) {
			queryRequest.setRangeKeyCondition(rangeKeyCondition);
		}

		if (lastKeyEvaluated != null) {
			queryRequest.setExclusiveStartKey(lastKeyEvaluated);
		}

		QueryResult result = dynamoClient.query(queryRequest);

		List<Map<String, AttributeValue>> itemList = result.getItems();
		List<NodeLineage> descList = new ArrayList<NodeLineage>(itemList.size());
		for (Map<String, AttributeValue> item : itemList) {
			// It does not matter which mapper
			DboNodeLineage dbo = readMapper.marshallIntoObject(DboNodeLineage.class, item);
			NodeLineage lineage = new NodeLineage(dbo);
			descList.add(lineage);
		}
		return descList;
	}

	/**
	 * Creates the key of the last item to get the next page.
	 */
	private Key createPagingKey(String nodeId, String descId) throws IncompletePathException {

		if (descId == null) {
			return null; // Return a null key which fetches the first page
		}

		int distance = -1;
		List<NodeLineage> path = NodeTreeDaoUtils.getCompletePathFromRoot(descId, readMapper);
		for (int i = 0; i < path.size(); i++) {
			NodeLineage lineage = path.get(i);
			String ancId = lineage.getAncestorOrDescendantId();
			if (nodeId.equals(ancId)) {
				distance = path.size() - i;
				break;
			}
		}
		if (distance < 0) {
			throw new IncompletePathException("Node " + nodeId + " is not on the ancestor path of " + descId);
		}

		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		AttributeValue hashKeyValue = new AttributeValue().withS(hashKey);
		String rangeKey = DboNodeLineage.createRangeKey(distance, descId);
		AttributeValue rangeKeyValue = new AttributeValue().withS(rangeKey);
		Key lastKeyEvaluated = new Key().withHashKeyElement(hashKeyValue).withRangeKeyElement(rangeKeyValue);
		return lastKeyEvaluated;
	}
}
