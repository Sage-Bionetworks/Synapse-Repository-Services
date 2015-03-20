package org.sagebionetworks.dynamo.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class DynamoTableConfigTest {

	@Test
	public void testDynamoKey() {

		DynamoTableConfig.DynamoKey key = new DynamoTableConfig.DynamoKey("name", KeyType.HASH, ScalarAttributeType.S);
		assertEquals("name", key.getKeyName());
		assertEquals(KeyType.HASH, key.getKeyType());
		assertEquals(ScalarAttributeType.S, key.getAttributeType());

		try {
			key = new DynamoTableConfig.DynamoKey("name", null, ScalarAttributeType.S);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			key = new DynamoTableConfig.DynamoKey(null, KeyType.HASH, ScalarAttributeType.S);
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			key = new DynamoTableConfig.DynamoKey("name", KeyType.HASH, null);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testDynamoKeySchema() {

		DynamoTableConfig.DynamoKey hashKey = new DynamoTableConfig.DynamoKey("hash", KeyType.HASH, ScalarAttributeType.N);
		DynamoTableConfig.DynamoKey rangeKey = new DynamoTableConfig.DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
		DynamoTableConfig.DynamoKeySchema keySchema = new DynamoTableConfig.DynamoKeySchema(hashKey, rangeKey);
		Assert.assertEquals("hash", keySchema.getHashKey().getKeyName());
		Assert.assertEquals(KeyType.HASH, keySchema.getHashKey().getKeyType());
		Assert.assertEquals(ScalarAttributeType.N, keySchema.getHashKey().getAttributeType());
		Assert.assertEquals("range", keySchema.getRangeKey().getKeyName());
		Assert.assertEquals(KeyType.RANGE, keySchema.getRangeKey().getKeyType());
		Assert.assertEquals(ScalarAttributeType.S, keySchema.getRangeKey().getAttributeType());

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

		DynamoTableConfig.DynamoKey hashKey = new DynamoTableConfig.DynamoKey("hash", KeyType.HASH, ScalarAttributeType.S);
		DynamoTableConfig.DynamoKey rangeKey = new DynamoTableConfig.DynamoKey("range", KeyType.RANGE, ScalarAttributeType.S);
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
