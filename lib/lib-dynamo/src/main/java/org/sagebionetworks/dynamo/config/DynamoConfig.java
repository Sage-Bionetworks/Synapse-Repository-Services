package org.sagebionetworks.dynamo.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.sagebionetworks.dynamo.dao.DboNodeLineage;

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
		DynamoThroughput throughput = new DynamoThroughput(15L, 5L);
		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		tableList.add(table);

		this.tableList = Collections.unmodifiableList(tableList);
	}

	Iterable<DynamoTableConfig> listTables() {
		return this.tableList;
	}

	private final List<DynamoTableConfig> tableList;
}
