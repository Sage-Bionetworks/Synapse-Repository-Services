package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageQueueImplIntegrationTest {

	@Autowired
	private AmazonSQSClient awsSQSClient;

	@Autowired
	private AmazonSNSClient awsSNSClient;
	
	//	This one has dead letter
	@Autowired
	private MessageQueueImpl testMessageQueue1;
	
	@Autowired
	private MessageQueueImpl testMessageQueue2;
	
	@Autowired
	private QueueServiceDao qSvcDao;
	
	@Before
	public void setup() throws Exception {
		cleanupQueues();
	}

	@Test
	@Ignore
	public void testSetupDL() throws InterruptedException {
		ListQueuesResult lqRes = awsSQSClient.listQueues("testQ");
		assertNotNull(lqRes);
		// 3 queues expected
		assertEquals(3, lqRes.getQueueUrls().size());
		// Queue URLs from SQS should match what's in the object
		GetQueueUrlResult quRes = awsSQSClient.getQueueUrl(testMessageQueue1.getQueueName());
		assertNotNull(quRes);
		assertEquals(testMessageQueue1.getQueueUrl(), quRes.getQueueUrl());
		quRes = awsSQSClient.getQueueUrl(testMessageQueue1.getDeadLetterQueueName());
		assertEquals(testMessageQueue1.getDeadLetterQueueUrl(), quRes.getQueueUrl());
		
		// Verify that we can receive a msg once, then it's put on the dead letter queue
		generateMessage(testMessageQueue1);
		verifyBehavior(testMessageQueue1, true);

		// No dead letter queue case
		assertNull(testMessageQueue2.getDeadLetterQueueName());

		// Queue URLs from SQS should match what's in the object
		GetQueueUrlResult quRes2 = awsSQSClient.getQueueUrl(testMessageQueue2.getQueueName());
		assertNotNull(quRes2);
		assertEquals(testMessageQueue2.getQueueUrl(), quRes2.getQueueUrl());
		
		generateMessage(testMessageQueue2);
		verifyBehavior(testMessageQueue2, false);

	}

	private void generateMessage(MessageQueueImpl msgQImpl) throws InterruptedException {
		SendMessageRequest smReq = new SendMessageRequest();
		smReq.setQueueUrl(msgQImpl.getQueueUrl());
		smReq.setDelaySeconds(1);
		smReq.setMessageBody(new String("theMessageBody"));
		awsSQSClient.sendMessage(smReq);
		List<Message> msgs = waitForMsg(msgQImpl.getQueueUrl());
		assertNotNull(msgs);
		assertEquals(1, msgs.size());
		assertEquals("theMessageBody", msgs.get(0).getBody());
	}
	
	private void verifyBehavior(MessageQueueImpl msgQImpl, boolean hasDeadLetter) throws InterruptedException {
		List<Message> msgs = waitForMsg(msgQImpl.getQueueUrl());
		if (hasDeadLetter) {
			// Waited > 1 sec, msg should not be back on queue
			assertNotNull(msgs);
			assertEquals(0, msgs.size());
			// Now it should be on the dead letter queue
			List<Message> msgsDL = waitForMsg(msgQImpl.getDeadLetterQueueUrl());
			assertNotNull(msgsDL);
			assertEquals(1, msgsDL.size());
			assertEquals("theMessageBody", msgsDL.get(0).getBody());
		} else {
			// Waited > 1 sec, msg should be back on queue
			assertNotNull(msgs);
			assertEquals(1, msgs.size());
			assertEquals("theMessageBody", msgs.get(0).getBody());
		}
	}
	
	private List<Message> waitForMsg(String qUrl) throws InterruptedException {
		final int MAX_RETRY = 15;
		final long SLEEP_MS = 3000L;
		ReceiveMessageRequest rmReq = new ReceiveMessageRequest().withQueueUrl(qUrl).withVisibilityTimeout(1);
		ReceiveMessageResult rmRes = null;
		List<Message> msgs = null;
		for (int i = 0; i < MAX_RETRY; i++) {
			rmRes = awsSQSClient.receiveMessage(rmReq);
			msgs = rmRes.getMessages();
			if (msgs.size() > 0)
				break;
			Thread.sleep(SLEEP_MS);
		}
		return msgs;
	}
	
	private void cleanupQueues() {
		PurgeQueueRequest pqReq = new PurgeQueueRequest().withQueueUrl(testMessageQueue1.getQueueUrl());
		awsSQSClient.purgeQueue(pqReq);
		pqReq.setQueueUrl(testMessageQueue1.getDeadLetterQueueUrl());
		awsSQSClient.purgeQueue(pqReq);
		pqReq.setQueueUrl(testMessageQueue2.getQueueUrl());
		awsSQSClient.purgeQueue(pqReq);
	}

}
