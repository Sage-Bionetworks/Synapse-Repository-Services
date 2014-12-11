package org.sagebionetworks.dynamo.config;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

public class DynamoTableConfigTest {

	@Test
	public void testDynamoKey() {

		DynamoTableConfig.DynamoKey key = new DynamoTableConfig.DynamoKey("name", ScalarAttributeType.N);
		Assert.assertEquals("name", key.getKeyName());
		Assert.assertEquals(ScalarAttributeType.N, key.getKeyType());

		try {
			key = new DynamoTableConfig.DynamoKey("name", null);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			key = new DynamoTableConfig.DynamoKey(null, ScalarAttributeType.S);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testDynamoKeySchema() {

		DynamoTableConfig.DynamoKey hashKey = new DynamoTableConfig.DynamoKey("hash", ScalarAttributeType.N);
		DynamoTableConfig.DynamoKey rangeKey = new DynamoTableConfig.DynamoKey("range", ScalarAttributeType.S);
		DynamoTableConfig.DynamoKeySchema keySchema = new DynamoTableConfig.DynamoKeySchema(hashKey, rangeKey);
		Assert.assertEquals("hash", keySchema.getHashKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.N, keySchema.getHashKey().getKeyType());
		Assert.assertEquals("range", keySchema.getRangeKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.S, keySchema.getRangeKey().getKeyType());

		keySchema = new DynamoTableConfig.DynamoKeySchema(hashKey, null);
		Assert.assertNotNull(keySchema);

		try {
			keySchema = new DynamoTableConfig.DynamoKeySchema(null, rangeKey);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testDynamoThroughput() {

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoTableConfig.DynamoThroughput throughput = new DynamoTableConfig.DynamoThroughput(read, write);
		Assert.assertEquals(read.longValue(), throughput.getReadThroughput().longValue());
		Assert.assertEquals(write.longValue(), throughput.getWriteThroughput().longValue());

		try {
			throughput = new DynamoTableConfig.DynamoThroughput(read, null);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			throughput = new DynamoTableConfig.DynamoThroughput(null, write);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testDynamoTable() {

		String tableName = "name";

		DynamoTableConfig.DynamoKey hashKey = new DynamoTableConfig.DynamoKey("hash", ScalarAttributeType.N);
		DynamoTableConfig.DynamoKey rangeKey = new DynamoTableConfig.DynamoKey("range", ScalarAttributeType.S);
		DynamoTableConfig.DynamoKeySchema keySchema = new DynamoTableConfig.DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoTableConfig.DynamoThroughput throughput = new DynamoTableConfig.DynamoThroughput(read, write);

		DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		Assert.assertEquals(stackPrefix + tableName, table.getTableName());
		Assert.assertSame(keySchema, table.getKeySchema());
		Assert.assertSame(throughput, table.getThroughput());

		try {
			table = new DynamoTableConfig(null, keySchema, throughput);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			table = new DynamoTableConfig(tableName, null, throughput);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			table = new DynamoTableConfig(tableName, keySchema, null);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
}
