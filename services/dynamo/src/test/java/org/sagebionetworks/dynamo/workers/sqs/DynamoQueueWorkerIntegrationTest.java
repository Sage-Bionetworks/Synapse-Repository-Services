package org.sagebionetworks.dynamo.workers.sqs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dynamo-spb.xml" })
public class DynamoQueueWorkerIntegrationTest {

	@Autowired
	private MessageReceiver dynamoQueueMessageRetriever;

	@Before
	public void before() throws Exception {
		// Empty the dynmao queue
		int count = this.dynamoQueueMessageRetriever.triggerFired();
		while (count > 0) {
			count = this.dynamoQueueMessageRetriever.triggerFired();
		}
	}

	@Test
	public void test() {
		Assert.assertTrue(true);
	}
}
