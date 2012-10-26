package org.sagebionetworks.dynamo;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dynamo-dao-spb.xml" })
public class DynamoSetupImplAutowireTest {

	@Autowired
	private DynamoSetup dynamoSetup;

	@Autowired
	private AmazonDynamoDB dynamoClient;

	@Test
	public void testSetup() {
		// At this point, DynamoSetup has been initialized, all the tables should be ACTIVE
		DynamoConfig config = new DynamoConfig();
		Iterable<DynamoTable> tables = config.listTables();
		for (DynamoTable table : tables) {
			String tableName = table.getTableName();
			DescribeTableRequest dtRequest = new DescribeTableRequest().withTableName(tableName);
			TableDescription tableDesc = this.dynamoClient.describeTable(dtRequest).getTable();
			TableStatus tableStatus = TableStatus.fromValue(tableDesc.getTableStatus());
			Assert.assertEquals(TableStatus.ACTIVE, tableStatus);
		}
	}
}
