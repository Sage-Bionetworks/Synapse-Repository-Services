package org.sagebionetworks.dynamo.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.dynamo.DynamoTimeoutException;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

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
					// delete and recreate the table
					this.deleteTable(existingTable.getTableName());
					if (!TimeUtils.waitForExponential(timeoutInMillis, 1000L, existingTable.getTableName(), new Predicate<String>() {
						@Override
						public boolean apply(String tableName) {
							try {
								describeTable(tableName);
								return false;
							} catch (ResourceNotFoundException e) {
								return true;
							}
						}
					})) {
						throw new DynamoTimeoutException("Table " + tableName
								+ " already exists but has different throughput and deletion did not happen in time.");
					}
					this.createTable(configTable);
					tablesToWatch.add(tableName);
				} else if (!this.hasSameThroughput(existingTable, configTable)) {
					// If they have different throughput, update the throughput
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

		if (!TimeUtils.waitForExponential(timeoutInMillis, 1000L, tablesToWatch, new Predicate<List<String>>() {
			@Override
			public boolean apply(List<String> tablesToWatch) {
				// Check readiness
				for (String tableName : tablesToWatch) {
					DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
					TableDescription tableDesc = dynamoClient.describeTable(request).getTable();
					TableStatus tableStatus = TableStatus.fromValue(tableDesc.getTableStatus());
					if (!TableStatus.ACTIVE.equals(tableStatus)) {
						return false;
					}
				}
				return true;
			}
		})) {
			throw new DynamoTimeoutException("Tables are not ready within the specified timeout of " + timeoutInMillis + " milliseconds.");
		}
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
		Pair<List<KeySchemaElement>, List<AttributeDefinition>> keySchema = this.getKeySchema(table);
		ProvisionedThroughput provisionedThroughput = this.getProvisionedThroughput(table);

		CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(keySchema.getFirst())
				.withAttributeDefinitions(keySchema.getSecond()).withProvisionedThroughput(provisionedThroughput);

		this.dynamoClient.createTable(request);
	}

	/**
	 * Deletes an existing table
	 */
	void deleteTable(String tableName) {
		this.dynamoClient.deleteTable(new DeleteTableRequest().withTableName(tableName));
	}

	/**
	 * Updates an existing table. This methods does not block. It returns while the table is being updated.
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
	private Pair<List<KeySchemaElement>, List<AttributeDefinition>> getKeySchema(DynamoTableConfig table) {

		assert table != null;

		DynamoKeySchema kSchema = table.getKeySchema();
		DynamoKey hKey = kSchema.getHashKey();
		KeySchemaElement hashKey = new KeySchemaElement().withAttributeName(hKey.getKeyName()).withKeyType(hKey.getKeyType());
		AttributeDefinition hashKeyAttributeDefinition = new AttributeDefinition(hKey.getKeyName(), hKey.getAttributeType());

		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKey);
		List<AttributeDefinition> attributes = Lists.newArrayList(hashKeyAttributeDefinition);

		DynamoKey rKey = kSchema.getRangeKey();
		if (rKey != null) {
			KeySchemaElement rangeKey = new KeySchemaElement().withAttributeName(rKey.getKeyName()).withKeyType(rKey.getKeyType());
			keySchema.add(rangeKey);
			AttributeDefinition rangeKeyAttributeDefinition = new AttributeDefinition(rKey.getKeyName(), rKey.getAttributeType());
			attributes.add(rangeKeyAttributeDefinition);
		}

		return Pair.create(keySchema, attributes);
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

	private static final Comparator<AttributeDefinition> ATTRIBUTE_DEFINITION_COMPARATOR = new Comparator<AttributeDefinition>() {
		@Override
		public int compare(AttributeDefinition o1, AttributeDefinition o2) {
			return o1.getAttributeName().compareTo(o2.getAttributeName());
		}
	};

	/**
	 * Whether the existing table and the table defined in configuration has the same key schema.
	 */
	private boolean hasSameKeySchema(TableDescription existingTable, DynamoTableConfig configTable) {

		assert existingTable != null;
		assert configTable != null;

		// The key schema must be the same
		Pair<List<KeySchemaElement>, List<AttributeDefinition>> configKeySchema = this.getKeySchema(configTable);
		Pair<List<KeySchemaElement>, List<AttributeDefinition>> existingSchema = Pair.create(existingTable.getKeySchema(),
				existingTable.getAttributeDefinitions());
		Collections.sort(configKeySchema.getSecond(), ATTRIBUTE_DEFINITION_COMPARATOR);
		Collections.sort(existingSchema.getSecond(), ATTRIBUTE_DEFINITION_COMPARATOR);
		return existingSchema.equals(configKeySchema);
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
