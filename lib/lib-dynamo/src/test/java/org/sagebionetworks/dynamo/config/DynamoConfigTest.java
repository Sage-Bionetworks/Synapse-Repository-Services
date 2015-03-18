package org.sagebionetworks.dynamo.config;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;

import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class DynamoConfigTest {
	@Test
	public void test() {
		DynamoConfig config = new DynamoConfig();
		Iterable<DynamoTableConfig> tables = config.listTables();
		Assert.assertNotNull(tables);

		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		boolean foundDboNodeLineage = false;
		for (DynamoTableConfig table : tables) {
			if (table.getTableName().equals(stackPrefix + DboNodeLineage.TABLE_NAME)) {
				foundDboNodeLineage = true;
				Assert.assertNotNull(table.getKeySchema());
				Assert.assertNotNull(table.getKeySchema().getHashKey());
				Assert.assertEquals(DboNodeLineage.HASH_KEY_NAME, table.getKeySchema().getHashKey().getKeyName());
				Assert.assertEquals(KeyType.HASH, table.getKeySchema().getHashKey().getKeyType());
				Assert.assertEquals(ScalarAttributeType.S, table.getKeySchema().getHashKey().getAttributeType());
				Assert.assertNotNull(table.getKeySchema().getRangeKey());
				Assert.assertEquals(DboNodeLineage.RANGE_KEY_NAME, table.getKeySchema().getRangeKey().getKeyName());
				Assert.assertEquals(KeyType.RANGE, table.getKeySchema().getRangeKey().getKeyType());
				Assert.assertEquals(ScalarAttributeType.S, table.getKeySchema().getRangeKey().getAttributeType());
				Assert.assertNotNull(table.getThroughput());
				Assert.assertTrue(table.getThroughput().getReadThroughput().longValue() >= 1L);
				Assert.assertTrue(table.getThroughput().getWriteThroughput().longValue() >= 1L);
				Assert.assertTrue(table.getThroughput().getReadThroughput().longValue() < 100L);
				Assert.assertTrue(table.getThroughput().getWriteThroughput().longValue() < 100L);
			}
		}
		assertTrue(foundDboNodeLineage);
	}
}
