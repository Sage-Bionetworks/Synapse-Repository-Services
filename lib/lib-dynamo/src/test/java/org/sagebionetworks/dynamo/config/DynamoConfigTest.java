package org.sagebionetworks.dynamo.config;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.DboNodeLineage;

import com.amazonaws.services.dynamodb.model.ScalarAttributeType;

public class DynamoConfigTest {
	@Test
	public void test() {
		DynamoConfig config = new DynamoConfig();
		Iterable<DynamoTableConfig> tables = config.listTables();
		Assert.assertNotNull(tables);
		Iterator<DynamoTableConfig> it = tables.iterator();
		DynamoTableConfig table = it.next();
		Assert.assertEquals(StackConfiguration.getStack() + "-" + DboNodeLineage.TABLE_NAME, table.getTableName());
		Assert.assertNotNull(table.getKeySchema());
		Assert.assertNotNull(table.getKeySchema().getHashKey());
		Assert.assertEquals(DboNodeLineage.HASH_KEY_NAME, table.getKeySchema().getHashKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.S, table.getKeySchema().getHashKey().getKeyType());
		Assert.assertNotNull(table.getKeySchema().getRangeKey());
		Assert.assertEquals(DboNodeLineage.RANGE_KEY_NAME, table.getKeySchema().getRangeKey().getKeyName());
		Assert.assertEquals(ScalarAttributeType.S, table.getKeySchema().getRangeKey().getKeyType());
		Assert.assertNotNull(table.getThroughput());
		Assert.assertTrue(table.getThroughput().getReadThroughput().longValue() >= 1L);
		Assert.assertTrue(table.getThroughput().getWriteThroughput().longValue() >= 1L);
	}
}
