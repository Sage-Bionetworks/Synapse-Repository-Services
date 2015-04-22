package org.sagebionetworks.dynamo.dao;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

	/**
	 * Helper to make make a unique item list according to comparator. Needed for batch deletes, since dynamo does not
	 * allow duplicate keys
	 * 
	 * @param items
	 * @param comparator
	 * @return
	 */
	protected <T> List<T> uniqueify(List<T> items, Comparator<T> comparator) {
		Set<T> set = Sets.newTreeSet(comparator);
		set.addAll(items);
		return Lists.newArrayList(set);
	}
}
