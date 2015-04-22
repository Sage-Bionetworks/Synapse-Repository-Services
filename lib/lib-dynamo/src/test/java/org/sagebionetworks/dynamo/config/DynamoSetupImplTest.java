package org.sagebionetworks.dynamo.config;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTimeoutException;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.sagebionetworks.util.DefaultClock;
import org.sagebionetworks.util.ReflectionStaticTestUtils;
import org.sagebionetworks.util.TestClock;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.google.common.collect.Lists;

public class DynamoSetupImplTest {

	private AmazonDynamoDB mockDynamoClient;
	private DynamoSetupImpl dynamoSetup;
	private TestClock testClock = new TestClock();

	@Before
	public void before() {
		this.mockDynamoClient = mock(AmazonDynamoDB.class);
		this.dynamoSetup = new DynamoSetupImpl();
		dynamoSetup.setDynamoEnabled(true);
		ReflectionTestUtils.setField(this.dynamoSetup, "dynamoClient", this.mockDynamoClient);
		ReflectionStaticTestUtils.setStaticField(TimeUtils.class, "clock", testClock);

	}

	@After
	public void after() throws Exception {
		ReflectionStaticTestUtils.setStaticField(TimeUtils.class, "clock", new DefaultClock());
	}

	@Test
	public void testCreateTable() {

		String tableName = "testCreateTable";

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);

		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		this.dynamoSetup.createTable(table);

		KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName("hash").withKeyType(KeyType.HASH);

		KeySchemaElement rangeKeyElement = new KeySchemaElement().withAttributeName("range").withKeyType(KeyType.RANGE);

		List<KeySchemaElement> kSchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);

		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);

		tableName = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-" + tableName;
		CreateTableRequest request = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(kSchema)
				.withProvisionedThroughput(pThroughput)
				.withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S));

		verify(this.mockDynamoClient, times(1)).createTable(request);
	}

	@Test
	public void testUpdateThroughput() {

		String tableName = "testUpdateThroughput";

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
		DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);

		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		this.dynamoSetup.updateProvisionedThroughput(table);

		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);

		tableName = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-" + tableName;
		UpdateTableRequest request = new UpdateTableRequest()
				.withTableName(tableName)
				.withProvisionedThroughput(pThroughput);

		verify(this.mockDynamoClient, times(1)).updateTable(request);
	}

	@Test
	public void testSetupCreateTable() {

		String tableName = "oneTableToBeCreated";

		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(new ArrayList<String>());
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
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
				.withKeyType(KeyType.HASH);
		KeySchemaElement rangeKeyElement = new KeySchemaElement()
				.withAttributeName("range")
				.withKeyType(KeyType.RANGE);
		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);
		ProvisionedThroughput pThroughput = new ProvisionedThroughput()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		tableName = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-" + tableName;
		CreateTableRequest ctRequest = new CreateTableRequest()
				.withTableName(tableName)
				.withKeySchema(keySchema)
				.withProvisionedThroughput(pThroughput).withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S));
		verify(this.mockDynamoClient, times(1)).createTable(ctRequest);
	}

	@Test
	public void testSetupUpdateTable() {

		String tableName = "oneTableToBeUpdated";
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				stackPrefix + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName("hash").withKeyType(KeyType.HASH);
		KeySchemaElement rangeKeyElement = new KeySchemaElement().withAttributeName("range").withKeyType(KeyType.RANGE);
		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);
		ProvisionedThroughputDescription throughputDesc = new ProvisionedThroughputDescription()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema)
				.withProvisionedThroughput(throughputDesc)
				.withTableStatus(TableStatus.ACTIVE)
				.withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S));
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(stackPrefix + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
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
				.withTableName(stackPrefix + tableName)
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

	@Test(expected = DynamoTimeoutException.class)
	public void testSetupDynamoDiffernetTableExistsException() {

		String tableName = "tableOfDiffKeySchema";
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				stackPrefix + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName("hash").withKeyType(KeyType.HASH);
		KeySchemaElement rangeKeyElement = new KeySchemaElement().withAttributeName("range").withKeyType(KeyType.RANGE);
		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema)
				.withTableName(stackPrefix + tableName)
				.withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S));
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(stackPrefix + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey("rangeDiff", KeyType.RANGE, ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);
		when(mockDynamoClient.createTable(any(CreateTableRequest.class))).thenReturn(null);

		long before = testClock.currentTimeMillis();
		try {
			this.dynamoSetup.setup(config);
		} finally {
			assertTrue("Should timeout after " + (before + DynamoSetup.TIMEOUT_IN_MILLIS) + " but was " + testClock.currentTimeMillis(),
					before + DynamoSetup.TIMEOUT_IN_MILLIS < testClock.currentTimeMillis());
		}
	}

	@Test
	public void testSetupDynamoDiffernetTableExists() {

		String tableName = "tableOfDiffKeySchema";
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(stackPrefix + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName("hash").withKeyType(KeyType.HASH);
		KeySchemaElement rangeKeyElement = new KeySchemaElement().withAttributeName("range").withKeyType(KeyType.RANGE);
		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);
		TableDescription tableDesc = new TableDescription().withKeySchema(keySchema).withTableName(stackPrefix + tableName)
				.withTableStatus("ACTIVE")
				.withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S));
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(stackPrefix + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey("rangeDiff", KeyType.RANGE, ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);
		when(mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult, dtResult, dtResult).thenThrow(new ResourceNotFoundException(""))
				.thenReturn(dtResult);

		long before = testClock.currentTimeMillis();
		this.dynamoSetup.setup(config);
		assertTrue("Should have retried for at least 2 seconds but was " + testClock.currentTimeMillis(),
				before + 2000 <= testClock.currentTimeMillis());
	}

	@Test(expected=DynamoTableExistsException.class)
	public void testSetupDynamoTableExistsException2() {

		String tableName = "tableOfDiffThroughput";
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				stackPrefix + tableName);
		KeySchemaElement hashKeyElement = new KeySchemaElement().withAttributeName("hash").withKeyType(KeyType.HASH);
		KeySchemaElement rangeKeyElement = new KeySchemaElement().withAttributeName("range").withKeyType(KeyType.RANGE);
		List<KeySchemaElement> keySchema = Lists.newArrayList(hashKeyElement, rangeKeyElement);
		ProvisionedThroughputDescription throughputDesc = new ProvisionedThroughputDescription()
				.withReadCapacityUnits(1L)
				.withWriteCapacityUnits(2L);
		TableDescription tableDesc = new TableDescription()
				.withKeySchema(keySchema)
				.withProvisionedThroughput(throughputDesc)
				.withAttributeDefinitions(new AttributeDefinition("hash", ScalarAttributeType.N),
						new AttributeDefinition("range", ScalarAttributeType.S))
				.withTableStatus(TableStatus.UPDATING); // This should trigger the exception
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(stackPrefix + tableName);
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.N);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
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
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";

		DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(
				stackPrefix + tableName);
		TableDescription tableDesc = new TableDescription()
				.withTableStatus(TableStatus.CREATING); // Simulate infinite delay
		DescribeTableResult dtResult = mock(DescribeTableResult.class);
		when(dtResult.getTable()).thenReturn(tableDesc);
		when(this.mockDynamoClient.describeTable(dtRequest)).thenReturn(dtResult);
		List<String> tableNames = new ArrayList<String>();
		ListTablesResult ltResult = mock(ListTablesResult.class);
		when(ltResult.getTableNames()).thenReturn(tableNames);
		when(this.mockDynamoClient.listTables()).thenReturn(ltResult);

		DynamoKey hashKey = new DynamoKey("hash", KeyType.HASH, ScalarAttributeType.S);
		DynamoKey rangeKey = new DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
		DynamoKeySchema kSchema = new DynamoKeySchema(hashKey, rangeKey);
		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoThroughput throughput = new DynamoThroughput(read, write);
		DynamoTableConfig tableFromConfig = new DynamoTableConfig(tableName, kSchema, throughput);
		List<DynamoTableConfig> tablesFromConfig = new ArrayList<DynamoTableConfig>();
		tablesFromConfig.add(tableFromConfig);
		DynamoConfig config = mock(DynamoConfig.class);
		when(config.listTables()).thenReturn(tablesFromConfig);

		long before = testClock.currentTimeMillis();
		try {
			this.dynamoSetup.setup(true, 1000, config);
		} finally {
			assertTrue("Should timeout after " + (before + 1000) + " but was " + testClock.currentTimeMillis(),
					before + 1000 <= testClock.currentTimeMillis());
		}
	}
}
