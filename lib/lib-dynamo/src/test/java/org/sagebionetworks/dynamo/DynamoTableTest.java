package org.sagebionetworks.dynamo;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

public class DynamoTableTest {

	@Test
	public void testDynamoKey() {

		DynamoTable.DynamoKey key = new DynamoTable.DynamoKey("name", ScalarAttributeType.N);
		Assert.assertEquals("name", key.getKeyName());
		Assert.assertEquals(ScalarAttributeType.N, key.getKeyType());

		try {
			key = new DynamoTable.DynamoKey("name", null);
			Assert.assertNull(key);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}

		try {
			key = new DynamoTable.DynamoKey(null, ScalarAttributeType.S);
			Assert.assertNull(key);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}
	}

	@Test
	public void testDynamoKeySchema() {

		DynamoTable.DynamoKey hashKey = new DynamoTable.DynamoKey("hash", ScalarAttributeType.N);
		DynamoTable.DynamoKey rangeKey = new DynamoTable.DynamoKey("range", ScalarAttributeType.S);
		DynamoTable.DynamoKeySchema keySchema = new DynamoTable.DynamoKeySchema(hashKey, rangeKey);
		Assert.assertEquals("hash", keySchema.getHashKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.N, keySchema.getHashKey().getKeyType());
		Assert.assertEquals("range", keySchema.getRangeKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.S, keySchema.getRangeKey().getKeyType());

		try {
			keySchema = new DynamoTable.DynamoKeySchema(hashKey, null);
			Assert.assertNull(keySchema);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}

		try {
			keySchema = new DynamoTable.DynamoKeySchema(null, rangeKey);
			Assert.assertNull(keySchema);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}
	}

	@Test
	public void testDynamoThroughput() {

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoTable.DynamoThroughput throughput = new DynamoTable.DynamoThroughput(read, write);
		Assert.assertEquals(read.longValue(), throughput.getReadThroughput().longValue());
		Assert.assertEquals(write.longValue(), throughput.getWriteThroughput().longValue());

		try {
			throughput = new DynamoTable.DynamoThroughput(read, null);
			Assert.assertNull(throughput);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}

		try {
			throughput = new DynamoTable.DynamoThroughput(null, write);
			Assert.assertNull(throughput);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}
	}

	@Test
	public void testDynamoTable() {

		String tableName = "name";

		DynamoTable.DynamoKey hashKey = new DynamoTable.DynamoKey("hash", ScalarAttributeType.N);
		DynamoTable.DynamoKey rangeKey = new DynamoTable.DynamoKey("range", ScalarAttributeType.S);
		DynamoTable.DynamoKeySchema keySchema = new DynamoTable.DynamoKeySchema(hashKey, rangeKey);

		Long read = Long.valueOf(1L);
		Long write = Long.valueOf(2L);
		DynamoTable.DynamoThroughput throughput = new DynamoTable.DynamoThroughput(read, write);

		DynamoTable table = new DynamoTable(tableName, keySchema, throughput);
		Assert.assertEquals(StackConfiguration.getStack() + "-" + tableName, table.getTableName());
		Assert.assertSame(keySchema, table.getKeySchema());
		Assert.assertSame(throughput, table.getThroughput());

		try {
			table = new DynamoTable(null, keySchema, throughput);
			Assert.assertNull(table);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}

		try {
			table = new DynamoTable(tableName, null, throughput);
			Assert.assertNull(table);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}

		try {
			table = new DynamoTable(tableName, keySchema, null);
			Assert.assertNull(table);
		} catch (NullPointerException e) {
			Assert.assertTrue(true);
		} catch (Throwable e) {
			Assert.fail();
		}
	}
}
