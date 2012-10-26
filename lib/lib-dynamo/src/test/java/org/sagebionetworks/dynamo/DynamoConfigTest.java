package org.sagebionetworks.dynamo;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

public class DynamoConfigTest {
	@Test
	public void test() {
		DynamoConfig config = new DynamoConfig();
		Iterable<DynamoTable> tables = config.listTables();
		Assert.assertNotNull(tables);
		Iterator<DynamoTable> it = tables.iterator();
		DynamoTable table = it.next();
		Assert.assertEquals(StackConfiguration.getStack() + "-" + "NodeTree", table.getTableName());
		Assert.assertNotNull(table.getKeySchema());
		Assert.assertNotNull(table.getKeySchema().getHashKey());
		Assert.assertEquals("NodeId", table.getKeySchema().getHashKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.N, table.getKeySchema().getHashKey().getKeyType());
		Assert.assertNotNull(table.getKeySchema().getRangeKey());
		Assert.assertEquals("PathFromRoot", table.getKeySchema().getRangeKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.S, table.getKeySchema().getRangeKey().getKeyType());
		Assert.assertNotNull(table.getThroughput());
		Assert.assertEquals(1L, table.getThroughput().getReadThroughput().longValue());
		Assert.assertEquals(1L, table.getThroughput().getWriteThroughput().longValue());
	}
}
