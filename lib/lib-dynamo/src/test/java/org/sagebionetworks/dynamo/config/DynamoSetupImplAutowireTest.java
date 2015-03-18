package org.sagebionetworks.dynamo.config;

import junit.framework.Assert;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DynamoSetupImplAutowireTest {

	@Autowired
	private AmazonDynamoDB dynamoClient;

	@Before
	public void before() {
		StackConfiguration config = new StackConfiguration();
		Assume.assumeTrue(config.getDynamoEnabled());
	}

	@Test
	public void testSetup() {
		// At this point, DynamoSetup has been initialized, all the tables should be ACTIVE
		DynamoConfig config = new DynamoConfig();
		Iterable<DynamoTableConfig> tables = config.listTables();
		for (DynamoTableConfig table : tables) {
			String tableName = table.getTableName();
			DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(tableName);
			TableDescription tableDesc = this.dynamoClient.describeTable(dtRequest).getTable();
			TableStatus tableStatus = TableStatus.fromValue(tableDesc.getTableStatus());
			Assert.assertEquals(TableStatus.ACTIVE, tableStatus);
		}
	}
}
