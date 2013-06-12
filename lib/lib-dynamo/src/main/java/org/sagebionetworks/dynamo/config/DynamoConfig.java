package org.sagebionetworks.dynamo.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

//TODO
//1. Define a JSON schema for DynamoDB tables
//2. List the DynamoDB tables in a JSON file
//3. Initialize the config from JSON
class DynamoConfig {

	DynamoConfig() {

		List<DynamoTableConfig> tableList = new ArrayList<DynamoTableConfig>();

		String tableName = DboNodeLineage.TABLE_NAME;
		DynamoKey hashKey = new DynamoKey(DboNodeLineage.HASH_KEY_NAME, ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey(DboNodeLineage.RANGE_KEY_NAME, ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);
		DynamoThroughput throughput = new DynamoThroughput(10L, 5L);
		if (this.isProdStack()) {
			throughput = new DynamoThroughput(150L, 75L);
		}
		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		tableList.add(table);

		this.tableList = Collections.unmodifiableList(tableList);
	}

	Iterable<DynamoTableConfig> listTables() {
		return this.tableList;
	}

	private boolean isProdStack() {
		String stack = StackConfiguration.getStack();
		if ("prod".equalsIgnoreCase(stack)) {
			return true;
		}
		return false;
	}

	private final List<DynamoTableConfig> tableList;
}
