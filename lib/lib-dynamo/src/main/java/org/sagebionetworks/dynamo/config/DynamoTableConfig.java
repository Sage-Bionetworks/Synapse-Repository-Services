package org.sagebionetworks.dynamo.config;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

// TODO
// 1. Define a JSON schema for DynamoDB tables
// 2. List the DynamoDB tables in a JSON file
// 3. Initialize the config from JSON
public final class DynamoTableConfig {

	public final static class DynamoKey {

		DynamoKey(String keyName, ScalarAttributeType keyType) {
			if (keyName == null) {
				throw new NullPointerException();
			}
			if (keyType == null) {
				throw new NullPointerException();
			}
			this.keyName = keyName;
			this.keyType = keyType;
		}

		public String getKeyName() {
			return keyName;
		}

		public ScalarAttributeType getKeyType() {
			return keyType;
		}

		private final String keyName;
		private final ScalarAttributeType keyType;
	}

	public final static class DynamoKeySchema {

		DynamoKeySchema(DynamoKey hashKey, DynamoKey rangeKey) {
			if (hashKey == null) {
				throw new NullPointerException();
			}
			if (rangeKey == null) {
				throw new NullPointerException();
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
				throw new NullPointerException();
			}
			if (writeThroughput == null) {
				throw new NullPointerException();
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
			throw new NullPointerException();
		}
		if (keySchema == null) {
			throw new NullPointerException();
		}
		if (throughput == null) {
			throw new NullPointerException();
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
