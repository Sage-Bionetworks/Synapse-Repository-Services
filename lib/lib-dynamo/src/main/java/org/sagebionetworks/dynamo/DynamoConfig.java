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

		// Stores hierarchical data using paths
		String tableName = "NodePath";
		// Hash key is a compound of node ID and the path type
		// Path has two types. One is from the root to the current code.
		// The other is from a leaf node to the current code.
		// For example, node id is 1234567, hash key can be "1234567_fromRoot"
		DynamoKey hashKey = new DynamoKey("NodeId_PathType", ScalarAttributeType.S);
		// Path as the range key. There are two types of path. One is from the root
		// to the current code. The other is from a leaf node to the current code.
		// Path type is encoded in the hash key.
		// For example, the path from root "4489/5213/11051" where "/" is the separator.
		// "11051" is the parent of this node and a child of "5213".
		// "5213" is the parent of "11051" and a child of "4489".
		// "4489" is the root. Another example, a path from a leaf node
		// "15891/11005", where "15891" is a leaf node and a child of "11005",
		// "11005" is a child of the current node.
		DynamoKey rangeKey = new DynamoKey("Path", ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);
		DynamoThroughput throughput = new DynamoThroughput(1L, 1L);
		DynamoTable table = new DynamoTable(tableName, keySchema, throughput);
		tableList.add(table);

		this.tableList = Collections.unmodifiableList(tableList);
	}

	Iterable<DynamoTable> listTables() {
		return this.tableList;
	}

	private final List<DynamoTable> tableList;
}
