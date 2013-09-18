package org.sagebionetworks.message.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
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
	}

	@Test
	public void testUnsentMessages() throws InterruptedException{
		// This will be added to the change table, but will not be sent
		List<ChangeMessage> message = unsentMessageQueuerTestHelper.createList(1, ObjectType.ENTITY, 10, 20);
		message = changeDAO.replaceChange(message);
		
		// List the unsent messages.
		List<ChangeMessage> unsent = changeDAO.listUnsentMessages(Long.MAX_VALUE);
		assertEquals(1, unsent.size());
		
		// Wait for message to be fired
		long start = System.currentTimeMillis();
		do{
			System.out.println("Waiting for UnsentMessagePopper timer to fire...");
			Thread.sleep(2000);
			long elpase = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for quartz timer to fire.",elpase < MAX_WAIT);
			
			// Get the messages
			unsent = changeDAO.listUnsentMessages(Long.MAX_VALUE);
		} while (unsent.size() > 0);
	}
}
