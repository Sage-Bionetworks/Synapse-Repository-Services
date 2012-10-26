package org.sagebionetworks.dynamo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.dynamo.DynamoTable.DynamoKey;
import org.sagebionetworks.dynamo.DynamoTable.DynamoKeySchema;
import org.sagebionetworks.dynamo.DynamoTable.DynamoThroughput;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

//TODO
//1. Define a JSON schema for DynamoDB tables
//2. List the DynamoDB tables in a JSON file
//3. Initialize the config from JSON
class DynamoConfig {

	DynamoConfig() {

		List<DynamoTable> tableList = new ArrayList<DynamoTable>();

		// Node ID as the hash key
		DynamoTable.DynamoKey hashKey = new DynamoKey("NodeId", ScalarAttributeType.N);
		// PathFromRoot as the range key
		// PathFromRoot is the path from the root node to this node.
		// The path is represented by node IDs separated by the
		// designated separator. For example, the path string
		// "/4489/5213/11051" where "/" is the separator.
		// "11051" is the parent of this node and a child of "5213".
		// "5213" is the parent of "11051" and a child of "4489".
		// "4489" is the root.
		DynamoKey rangeKey = new DynamoKey("PathFromRoot", ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);
		DynamoThroughput throughput = new DynamoThroughput(1L, 1L);
		DynamoTable table = new DynamoTable("NodeTree", keySchema, throughput);
		tableList.add(table);

		this.tableList = Collections.unmodifiableList(tableList);
	}

	Iterable<DynamoTable> listTables() {
		return this.tableList;
	}

	private final List<DynamoTable> tableList;
}
