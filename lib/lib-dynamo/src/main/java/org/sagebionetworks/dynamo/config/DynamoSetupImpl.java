package org.sagebionetworks.dynamo.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.dynamo.DynamoTimeoutException;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ListTablesResult;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;

public class DynamoSetupImpl implements DynamoSetup {
	
	private final Logger logger = LogManager.getLogger(DynamoSetupImpl.class);

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

	// Called by Spring to initialize
	public void initialize() {
		DynamoConfig config = new DynamoConfig();
		this.setup(config);
	}

	@Override
	public void setup(DynamoConfig config) {
		this.setup(true, DynamoSetup.TIMEOUT_IN_MILLIS, config);		
	}

	@Override
	public void setup(boolean blockOnCreation, long timeoutInMillis, DynamoConfig config) {
		if(!isDynamoEnabled){
			logger.debug("Dynamo is disabled so the dynamo tables will not be configured");
			return;
		}
		if (timeoutInMillis <= 0) {
			throw new IllegalArgumentException("Timeout cannot be zero or negative.");
		}
		if (config == null) {
			throw new NullPointerException();
		}

		// Tables being created and updated in this process
		List<String> tablesToWatch = new ArrayList<String>();

		// Create or update tables
		Set<String> existingTables = this.getNamesOfExistingTables();
		Iterable<DynamoTableConfig> configTables = config.listTables();
		for (DynamoTableConfig configTable : configTables) {
			String tableName = configTable.getTableName();
			if (!existingTables.contains(tableName)) {
				// If the table does not exist in DynamoDB, create it
				this.createTable(configTable);
				tablesToWatch.add(tableName);
			} else {
				// If the table already exists
				TableDescription existingTable = this.describeTable(tableName);
				// Make sure they have the same KeySchema
				if (!this.hasSameKeySchema(existingTable, configTable)) {
					throw new DynamoTableExistsException("Table " + tableName
							+ " already exists but has a different key schema.");
				}
				// If they have different throughput, update the throughput
				if (!this.hasSameThroughput(existingTable, configTable)) {
					// The table must be in ACTIVE status (i.e. ready to update)
					TableStatus status = TableStatus.fromValue(existingTable.getTableStatus());
					if (!TableStatus.ACTIVE.equals(status)) {
						throw new DynamoTableExistsException("Table " + tableName
							+ " already exists but has different throughput. The table's status "
							+ " is not ACTIVE and cannot update the throughput right now.");
					}
					this.updateProvisionedThroughput(configTable);
					tablesToWatch.add(tableName);
				} else {
					// The table has the same throughput
					// If it is not ACTIVE, add it to the to-watch list
					TableStatus status = TableStatus.fromValue(existingTable.getTableStatus());
					if (!TableStatus.ACTIVE.equals(status)) {
						tablesToWatch.add(tableName);
					}
				}
			}
		}

		if (!blockOnCreation) {
			// Return right away if we don't want to wait for the tables to be ready
			return;
		}

		// Otherwise wait for the tables to be ready
		long delay = timeoutInMillis / 60 + 1;
		long startTime = System.currentTimeMillis();
		long endTime = startTime + timeoutInMillis;
		while (System.currentTimeMillis() < endTime) {

			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			// Check readiness
			boolean allActive = true;
			for (String tableName : tablesToWatch) {
				DescribeTableRequest request = new DescribeTableRequest();
				request.setTableName(tableName);
				TableDescription tableDesc = this.dynamoClient.describeTable(request).getTable();
				TableStatus tableStatus = TableStatus.fromValue(tableDesc.getTableStatus());
				if (!TableStatus.ACTIVE.equals(tableStatus)) {
					allActive = false;
					break;
				}
			}

			if (allActive) {
				return;
			}
		}

		// We have timed out
		throw new DynamoTimeoutException("Tables are not ready within the specified timeout of "
					+  timeoutInMillis + " milliseconds.");
	}

	/**
	 * Creates a new table. The table must not already exist. This methods does not block.
	 * It returns while the table is being created.
	 */
	void createTable(DynamoTableConfig table) {

		if (table == null) {
			throw new NullPointerException();
		}

		String tableName = table.getTableName();
		KeySchema keySchema = this.getKeySchema(table);
		ProvisionedThroughput provisionedThroughput = this.getProvisionedThroughput(table);

		CreateTableRequest request = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(keySchema)
				.withProvisionedThroughput(provisionedThroughput);

		this.dynamoClient.createTable(request);
	}

	/**
	 * Updates an existing table. This methods does not block.
	 * It returns while the table is being updated.
	 */
	void updateProvisionedThroughput(DynamoTableConfig table) {

		if (table == null) {
			throw new NullPointerException();
		}

		ProvisionedThroughput provisionedThroughput = this.getProvisionedThroughput(table);
		UpdateTableRequest request = new UpdateTableRequest()
				.withTableName(table.getTableName())
				.withProvisionedThroughput(provisionedThroughput);

		this.dynamoClient.updateTable(request);
	}

	/**
	 * Maps DynamoKeySchema to DynamoDB's KeySchema.
	 */
	private KeySchema getKeySchema(DynamoTableConfig table) {

		assert table != null;

		DynamoKeySchema kSchema = table.getKeySchema();
		DynamoKey hKey = kSchema.getHashKey();
		KeySchemaElement hashKey = new KeySchemaElement()
				.withAttributeName(hKey.getKeyName())
				.withAttributeType(hKey.getKeyType());

		DynamoKey rKey = kSchema.getRangeKey();
		KeySchemaElement rangeKey = new KeySchemaElement()
				.withAttributeName(rKey.getKeyName())
				.withAttributeType(rKey.getKeyType());

		KeySchema keySchema = new KeySchema()
				.withHashKeyElement(hashKey)
				.withRangeKeyElement(rangeKey);

		return keySchema;
	}

	/**
	 * Maps DynamoThroughput to DynamoDB's ProvisionedThroughput.
	 */
	private ProvisionedThroughput getProvisionedThroughput(DynamoTableConfig table) {

		assert table != null;

		DynamoThroughput throughput = table.getThroughput();
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(throughput.getReadThroughput())
				.withWriteCapacityUnits(throughput.getWriteThroughput());

		return provisionedThroughput;
	}

	/**
	 * Gets throughput from DynamoDB's TableDescription.
	 */
	private ProvisionedThroughput getProvisionedThroughput(TableDescription tableDesc) {

		assert tableDesc != null;

		ProvisionedThroughputDescription throughputDesc = tableDesc.getProvisionedThroughput();
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(throughputDesc.getReadCapacityUnits())
				.withWriteCapacityUnits(throughputDesc.getWriteCapacityUnits());

		return provisionedThroughput;
	}

	private TableDescription describeTable(String tableName) {
		assert tableName != null;
		DescribeTableRequest request = new DescribeTableRequest();
		request.setTableName(tableName);
		TableDescription tableDescription =
				this.dynamoClient.describeTable(request).getTable();
		return tableDescription;
	}

	private Set<String> getNamesOfExistingTables() {
		ListTablesResult result = this.dynamoClient.listTables();
		List<String> tableNameList = result.getTableNames();
		Set<String> tableNameSet = new HashSet<String>(tableNameList);
		return tableNameSet;
	}

	/**
	 * Whether the existing table and the table defined in configuration has the same key schema.
	 */
	private boolean hasSameKeySchema(TableDescription existingTable, DynamoTableConfig configTable) {

		assert existingTable != null;
		assert configTable != null;

		// The key schema must be the same
		KeySchema configKeySchema = this.getKeySchema(configTable);
		KeySchema keySchema = existingTable.getKeySchema();
		return keySchema.equals(configKeySchema);
	}

	/**
	 * Whether the existing table and the table defined in configuration has the same throughput.
	 */
	private boolean hasSameThroughput(TableDescription existingTable, DynamoTableConfig configTable) {

		assert existingTable != null;
		assert configTable != null;

		ProvisionedThroughput existingThroughput = this.getProvisionedThroughput(existingTable);
		ProvisionedThroughput configThroughput = this.getProvisionedThroughput(configTable);
		return existingThroughput.equals(configThroughput);
	}
}
