package org.sagebionetworks.dynamo.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTimeoutException;
import org.sagebionetworks.dynamo.config.DynamoConfig;
import org.sagebionetworks.dynamo.config.DynamoSetupImpl;
import org.sagebionetworks.dynamo.config.DynamoTableConfig;
import org.sagebionetworks.dynamo.config.DynamoTableExistsException;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ListTablesResult;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodb.model.ScalarAttributeType;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;

public class DynamoSetupImplTest {

	private AmazonDynamoDB mockDynamoClient;
	private DynamoSetupImpl dynamoSetup;

	@Before
	public void before() {
		this.mockDynamoClient = mock(AmazonDynamoDB.class);
		this.dynamoSetup = new DynamoSetupImpl();
		ReflectionTestUtils.setField(this.dynamoSetup, "dynamoClient", this.mockDynamoClient);
	}

	@Test
	public void testCreateTable() {

		String tableName = "testCreateTable";

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);

		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		this.dynamoSetup.createTable(table);

		KeySchemaElement hashKeyElement = new KeySchemaElement()
				.withAttributeName("hash")
				.withAttributeType(ScalarAttributeType.N);

		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withAttributeType(ScalarAttributeType.S);

		KeySchema kSchema = new KeySchema()
				.withHashKeyElement(hashKeyElement)
				.withRangeKeyElement(rangeKeyElement);

		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);

		CreateTableRequest request = new CreateTableRequest()
				.withTableName(StackConfiguration.getStack() + "-" + tableName)
				.withKeySchema(kSchema)
				.withProvisionedThroughput(pThroughput);

		verify(this.mockDynamoClient, times(1)).createTable(request);
	}

	@Test
	public void testUpdateThroughput() {

		String tableName = "testUpdateThroughput";

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);

		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		this.dynamoSetup.updateProvisionedThroughput(table);

		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);

		UpdateTableRequest request = new UpdateTableRequest()
				.withTableName(StackConfiguration.getStack() + "-" + tableName)
				.withProvisionedThroughput(pThroughput);

		verify(this.mockDynamoClient, times(1)).updateTable(request);
	}

	@Test
	public void testSetupCreateTable() {

		String tableName = "oneTableToBeCreated";

		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(new ArrayList<String>());
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		this.dynamoSetup.setup(false, 1000, config);

		KeySchemaElement hashKeyElement = new KeySchemaElement()
				.withAttributeName("hash")
				.withAttributeType(ScalarAttributeType.N);
		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withAttributeType(ScalarAttributeType.S);
		KeySchema keySchema = new KeySchema()
				.withHashKeyElement(hashKeyElement)
				.withRangeKeyElement(rangeKeyElement);
		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		CreateTableRequest ctRequest = new CreateTableRequest()
				.withTableName(StackConfiguration.getStack() + "-" + tableName)
				.withKeySchema(keySchema)
				.withProvisionedThroughput(pThroughput);
		verify(this.mockDynamoClient, times(1)).createTable(ctRequest);
	}

	@Test
	public void testSetupUpdateTable() {

		String tableName = "oneTableToBeUpdated";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				StackConfiguration.getStack() + "-" + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement()
				.withAttributeName("hash")
				.withAttributeType(ScalarAttributeType.N);
		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withAttributeType(ScalarAttributeType.S);
		KeySchema keySchema = new KeySchema()
				.withHashKeyElement(hashKeyElement)
				.withRangeKeyElement(rangeKeyElement);
		ProvisionedThroughputDescription throughputDesc = new ProvisionedThroughputDescription()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema)
				.withProvisionedThroughput(throughputDesc)
				.withTableStatus(TableStatus.ACTIVE);
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(StackConfiguration.getStack() + "-" + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(3L); // Different write throughput -- increase by 1
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		this.dynamoSetup.setup(false, 1000, config);

		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(3L);
		UpdateTableRequest utRequest = new UpdateTableRequest()
				.withTableName(StackConfiguration.getStack() + "-" + tableName)
				.withProvisionedThroughput(pThroughput);
		verify(this.mockDynamoClient, times(1)).updateTable(utRequest);
	}

	@Test(expected=NullPointerException.class)
	public void testSetupNullPointerException() {
		this.dynamoSetup.setup(null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetupIllegalArgumentException() {
		this.dynamoSetup.setup(true, -1L, new DynamoConfig());
	}

	@Test(expected=DynamoTableExistsException.class)
	public void testSetupDynamoTableExistsException1() {

		String tableName = "tableOfDiffKeySchema";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				StackConfiguration.getStack() + "-" + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement()
				.withAttributeName("hash")
				.withAttributeType(ScalarAttributeType.N);
		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withAttributeType(ScalarAttributeType.S);
		KeySchema keySchema = new KeySchema()
				.withHashKeyElement(hashKeyElement)
				.withRangeKeyElement(rangeKeyElement);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema);
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(StackConfiguration.getStack() + "-" + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey("rangeDiff", ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		this.dynamoSetup.setup(config);
	}

	@Test(expected=DynamoTableExistsException.class)
	public void testSetupDynamoTableExistsException2() {

		String tableName = "tableOfDiffThroughput";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				StackConfiguration.getStack() + "-" + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement()
				.withAttributeName("hash")
				.withAttributeType(ScalarAttributeType.N);
		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withAttributeType(ScalarAttributeType.S);
		KeySchema keySchema = new KeySchema()
				.withHashKeyElement(hashKeyElement)
				.withRangeKeyElement(rangeKeyElement);
		ProvisionedThroughputDescription throughputDesc = new ProvisionedThroughputDescription()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema)
				.withProvisionedThroughput(throughputDesc)
				.withTableStatus(TableStatus.UPDATING); // This should trigger the exception
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(StackConfiguration.getStack() + "-" + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(2L); // Different throughput -- increase read by 1
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		this.dynamoSetup.setup(config);
	}

	@Test(expected=DynamoTimeoutException.class)
	public void testSetupDynamoTimeoutException() throws Exception {

		String tableName = "testSetupDynamoTimeoutException";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				StackConfiguration.getStack() + "-" + tableName);
		TableDescription tableDesc = new TableDescription()
				.withTableStatus(TableStatus.CREATING); // Simulate infinite delay
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		this.dynamoSetup.setup(true, 1000, config);
	}
}
