package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.dynamo.dao.DynamoDaoBaseImpl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.google.common.collect.Maps;

/**
 * Implementation based on ancestor-descendant and descendant-ancestor pointers.
 */
public class NodeTreeQueryDaoImpl extends DynamoDaoBaseImpl implements NodeTreeQueryDao {

	private final DynamoDBMapper readMapper;

	public NodeTreeQueryDaoImpl(AmazonDynamoDB dynamoClient) {
		super(dynamoClient);

		DynamoDBMapperConfig mapperConfig = NodeLineageMapperConfig.getMapperConfig();
		this.readMapper = new DynamoDBMapper(dynamoClient, mapperConfig);
	}

	@Override
	public boolean isRoot(String nodeId) {
		validateDynamoEnabled();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		return NodeTreeDaoUtils.isRoot(nodeId, readMapper);
	}

	@Override
	public List<String> getAncestors(String nodeId) throws IncompletePathException {
		validateDynamoEnabled();
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
		validateDynamoEnabled();
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
		validateDynamoEnabled();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Map<String, AttributeValue> lastKeyEvaluated = createPagingKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = getDescendants(nodeId, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	@Override
	public List<String> getDescendants(String nodeId, int generation, int pageSize, String lastDescIdExcl) {
		validateDynamoEnabled();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Node ID cannot be null or empty.");
		}
		if (generation < 1) {
			throw new IllegalArgumentException("Must be at least 1 generation away.");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be greater than 0.");
		}

		Map<String, AttributeValue> lastKeyEvaluated = createPagingKey(nodeId, lastDescIdExcl);
		List<NodeLineage> descList = getDescendants(nodeId, generation, pageSize, lastKeyEvaluated);
		List<String> results = new ArrayList<String>(descList.size());
		for (NodeLineage desc : descList) {
			results.add(desc.getAncestorOrDescendantId());
		}
		return results;
	}

	// Private Methods ////////////////////////////////////////////////////////////////////////////

	private List<NodeLineage> getDescendants(String nodeId, int pageSize, Map<String, AttributeValue> lastKeyEvaluated) {
		String hashKey = DboNodeLineage.createHashKey(nodeId, LineageType.DESCENDANT);
		return getDescendants(hashKey, null, pageSize, lastKeyEvaluated, false);
	}

	private List<NodeLineage> getDescendants(String nodeId, int distance,
 int pageSize, Map<String, AttributeValue> lastKeyEvaluated) {
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
	private List<NodeLineage> getDescendants(String hashKey, Condition rangeKeyCondition, int pageSize,
			Map<String, AttributeValue> lastKeyEvaluated, boolean consistentRead) {

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
				.withLimit(pageSize)
				.withConsistentRead(consistentRead);

		Map<String, Condition> keyConditions = Maps.newHashMap();
		keyConditions.put(DboNodeLineage.HASH_KEY_NAME,
				new Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(hashKeyAttr));
		queryRequest.setKeyConditions(keyConditions);
		if (rangeKeyCondition != null) {
			keyConditions.put(DboNodeLineage.RANGE_KEY_NAME, rangeKeyCondition);
		}

		if (lastKeyEvaluated != null) {
			queryRequest.setExclusiveStartKey(lastKeyEvaluated);
		}

		QueryResult result = getDynamoClient().query(queryRequest);

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
	private Map<String, AttributeValue> createPagingKey(String nodeId, String descId) throws IncompletePathException {

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
		Map<String, AttributeValue> key = Maps.newHashMap();
		key.put(DboNodeLineage.HASH_KEY_NAME, hashKeyValue);
		key.put(DboNodeLineage.RANGE_KEY_NAME, rangeKeyValue);
		return key;
	}
}
