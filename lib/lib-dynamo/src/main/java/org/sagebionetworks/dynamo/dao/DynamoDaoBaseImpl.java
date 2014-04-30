package org.sagebionetworks.dynamo.dao;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBMapper;

public abstract class DynamoDaoBaseImpl {

	private AmazonDynamoDB dynamoClient;
	private boolean isDynamoEnabled;

	protected DynamoDaoBaseImpl(AmazonDynamoDB dynamoClient) {
		if (dynamoClient == null) {
			throw new IllegalArgumentException("DynamoDB client cannot be null.");
		}
		this.dynamoClient = dynamoClient;
	}

	public boolean isDynamoEnabled() {
		return isDynamoEnabled;
	}

	public void setDynamoEnabled(boolean isDynamoEnabled) {
		this.isDynamoEnabled = isDynamoEnabled;
	}

	/**
	 * @throws UnsupportedOperationException when Dynamo is disabled
	 */
	public void validateDynamoEnabled() {
		if (!isDynamoEnabled)
			throw new UnsupportedOperationException("All Dynamo related features are disabled");
	}

	public AmazonDynamoDB getDynamoClient() {
		return dynamoClient;
	}
}
