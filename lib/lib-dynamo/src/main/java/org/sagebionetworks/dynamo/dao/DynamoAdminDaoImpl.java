package org.sagebionetworks.dynamo.dao;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;

public class DynamoAdminDaoImpl implements DynamoAdminDao {

	@Autowired
	private AmazonDynamoDB dynamoClient;
	
	private boolean isDynamoEnabled;

	@Override
	public boolean isDynamoEnabled() {
		return isDynamoEnabled;
	}
 
	public void setDynamoEnabled(boolean isDynamoEnabled) {
		this.isDynamoEnabled = isDynamoEnabled;
	}

	@Override
	public void clear(final String tableName,
			final String hashKeyName, final String rangeKeyName) {
		
		if(!isDynamoEnabled) throw new UnsupportedOperationException("All Dynamo related features are disabled");
		
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
		ScanResult scanResult = dynamoClient.scan(scanRequest);
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
			dynamoClient.deleteItem(deleteItemRequest);
		}
	}

}
