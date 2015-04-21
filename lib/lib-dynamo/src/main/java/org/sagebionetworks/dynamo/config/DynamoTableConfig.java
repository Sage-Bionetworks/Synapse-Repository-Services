package org.sagebionetworks.dynamo.config;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

// TODO
// 1. Define a JSON schema for DynamoDB tables
// 2. List the DynamoDB tables in a JSON file
// 3. Initialize the config from JSON
public final class DynamoTableConfig {

	public final static class DynamoKey {

		private final String keyName;
		private final KeyType keyType;
		private final ScalarAttributeType attributeType;

		DynamoKey(String keyName, KeyType keyType, ScalarAttributeType attributeType) {
			if (keyName == null) {
				throw new IllegalArgumentException("keyName is required");
			}
			if (keyType == null) {
				throw new IllegalArgumentException("keyType is required");
			}
			if (attributeType == null) {
				throw new IllegalArgumentException("attributeType is required");
			}
			this.keyName = keyName;
			this.keyType = keyType;
			this.attributeType = attributeType;
		}

		public String getKeyName() {
			return keyName;
		}

		public KeyType getKeyType() {
			return keyType;
		}

		public ScalarAttributeType getAttributeType() {
			return attributeType;
		}
	}

	public final static class DynamoKeySchema {

		DynamoKeySchema(DynamoKey hashKey, DynamoKey rangeKey) {
			if (hashKey == null) {
				throw new IllegalArgumentException("hashKey is required");
			}
			this.hashKey = hashKey;
			this.rangeKey = rangeKey;
		}

		public DynamoKey getHashKey() {
			return hashKey;
		}

		public DynamoKey getRangeKey() {
			return rangeKey;
		}

		private final DynamoKey hashKey;
		private final DynamoKey rangeKey;
	}

	final static class DynamoThroughput {

		DynamoThroughput(Long readThroughput, Long writeThroughput) {
			if (readThroughput == null) {
				throw new IllegalArgumentException("readThroughput is required");
			}
			if (writeThroughput == null) {
				throw new IllegalArgumentException("writeThroughput is required");
			}
			this.readThroughput = readThroughput;
			this.writeThroughput = writeThroughput;
		}

		Long getReadThroughput() {
			return readThroughput;
		}

		Long getWriteThroughput() {
			return writeThroughput;
		}

		private final Long readThroughput;
		private final Long writeThroughput;
	}

	DynamoTableConfig(String tableName, DynamoKeySchema keySchema,
			DynamoThroughput throughput) {
		if (tableName == null) {
			throw new IllegalArgumentException("tableName is required");
		}
		if (keySchema == null) {
			throw new IllegalArgumentException("keySchema is required");
		}
		if (throughput == null) {
			throw new IllegalArgumentException("throughput is required");
		}
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		this.tableName = stackPrefix + tableName;
		this.keySchema = keySchema;
		this.throughput = throughput;
	}

	String getTableName() {
		return tableName;
	}

	DynamoKeySchema getKeySchema() {
		return keySchema;
	}

	DynamoThroughput getThroughput() {
		return throughput;
	}

	private final String tableName;
	private final DynamoKeySchema keySchema;
	private final DynamoThroughput throughput;
}
