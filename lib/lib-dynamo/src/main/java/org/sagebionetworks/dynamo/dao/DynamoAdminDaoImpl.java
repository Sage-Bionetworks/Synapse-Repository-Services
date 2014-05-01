package org.sagebionetworks.dynamo.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;

public class DynamoAdminDaoImpl extends DynamoDaoBaseImpl implements DynamoAdminDao {

	@Override
	public boolean isDynamoEnabled() {
		return super.isDynamoEnabled();
	}

	public DynamoAdminDaoImpl(AmazonDynamoDB dynamoClient) {
		super(dynamoClient);
	}
	@Override
	public void clear(final String tableName,
			final String hashKeyName, final String rangeKeyName) {
		
		if(!isDynamoEnabled()) throw new UnsupportedOperationException("All Dynamo related features are disabled");
		
		if (tableName == null || tableName.isEmpty()) {
			throw new IllegalArgumentException("Table name cannot be null or empty.");
		}
		if (hashKeyName == null || hashKeyName.isEmpty()) {
			throw new IllegalArgumentException("Hash key name cannot be null or empty.");
		}
		if (rangeKeyName == null || rangeKeyName.isEmpty()) {
			throw new IllegalArgumentException("Range key name cannot be null or empty.");
		}

		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		final String fullTableName = stackPrefix + tableName;

		ScanRequest scanRequest = new ScanRequest()
				.withTableName(fullTableName)
				.withAttributesToGet(hashKeyName, rangeKeyName);
		ScanResult scanResult = getDynamoClient().scan(scanRequest);
		List<Map<String, AttributeValue>> items = scanResult.getItems();

		for (Map<String, AttributeValue> item : items) {
			AttributeValue hashKeyValue = item.get(hashKeyName);
			AttributeValue rangeKeyValue = item.get(rangeKeyName);
			Key key = new Key()
					.withHashKeyElement(hashKeyValue)
					.withRangeKeyElement(rangeKeyValue);
			DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
					.withTableName(fullTableName)
					.withKey(key);
			getDynamoClient().deleteItem(deleteItemRequest);
		}
	}

}
