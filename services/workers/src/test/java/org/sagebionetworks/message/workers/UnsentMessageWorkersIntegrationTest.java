package org.sagebionetworks.message.workers;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.AmazonSQSClient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UnsentMessageWorkersIntegrationTest {

	/**
	 * Enough time for both the Queuer (10 sec) and Popper (30 sec) to fire
	 * Plus some extra time just in case
	 */
	public static long MAX_WAIT = 1000*50; 

	@Autowired
	private UnsentMessageQueuer unsentMessageQueuer;
	
	@Autowired
	private MessageQueue unsentMessageQueue;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	@Autowired
	private AmazonSQSClient awsSQSClient;
	private String queueURL;

	@Autowired
	private UnsentMessageQueuerTestHelper unsentMessageQueuerTestHelper;
	
	@Before
	public void setup() throws Exception {
		queueURL = unsentMessageQueue.getQueueUrl();
		
		changeDAO.deleteAllChanges();
		unsentMessageQueuerTestHelper.emptyQueue(queueURL);
	}

	@After
	public void teardown() throws Exception {
		changeDAO.deleteAllChanges();
		unsentMessageQueuerTestHelper.emptyQueue(queueURL);
	}

	@Ignore
	@Test
	public void testRoundtrip() throws InterruptedException {
		// This will be added to the change table, but will not be sent
		// Create a sizable number of messages (~10% gaps)
		List<ChangeMessage> batch = unsentMessageQueuerTestHelper.createList(UnsentMessageQueuerTest.NUM_MESSAGES_TO_CREATE, 
				ObjectType.ENTITY, 0, 10 * UnsentMessageQueuerTest.NUM_MESSAGES_TO_CREATE);
		batch = changeDAO.replaceChange(batch);
		
		// List the unsent messages.
		List<ChangeMessage> unsent = changeDAO.listUnsentMessages(Long.MAX_VALUE);
		
		// Wait for message to be fired
		long start = System.currentTimeMillis();
		do{
			System.out.println("Waiting for UnsentMessagePopper timer to fire...");
			Thread.sleep(2000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for quartz timer to fire.", elapse < MAX_WAIT);
			
			// Get the messages
			unsent = changeDAO.listUnsentMessages(Long.MAX_VALUE);
		} while (unsent.size() > 0);
	}
	
	@Test
	public void testQueueMoreThan10Messages() throws Exception {
		// Make the range as small as possible, to send as many messages to SQS
		unsentMessageQueuer.setApproxRangeSize(1L);
		
		// Create more than 10 messages
		List<ChangeMessage> batch = unsentMessageQueuerTestHelper.createList(25, 
				ObjectType.ENTITY, 0, 40);
		batch = changeDAO.replaceChange(batch);
		
		// Make sure there are more than 10 messages
		List<?> messageBatch = unsentMessageQueuer.buildRangeBatch();
		assertTrue(messageBatch.size() > 10);
		
		// Should not get an error message from AWS
		unsentMessageQueuer.run();
	}
}
