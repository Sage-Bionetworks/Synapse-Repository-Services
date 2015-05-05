package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.Message;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageQueueImplIntegrationTest {

	@Autowired
	private AmazonSQSClient awsSQSClient;

	@Autowired
	private AmazonSNSClient awsSNSClient;
	
	@Autowired
	private MessageQueueImpl msgQImpl;
	
	@Autowired
	private QueueServiceDao qSvcDao;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
//		System.out.println("Deleting test queue...");
//		DeleteQueueRequest dqReq = new DeleteQueueRequest().withQueueUrl(msgQImpl.getQueueUrl());
//		awsSQSClient.deleteQueue(dqReq);
//		dqReq.setQueueUrl(msgQImpl.getDeadLetterQueueName());
//		awsSQSClient.deleteQueue(dqReq);
	}

	@Test
	public void testSetup() throws InterruptedException {
		ListQueuesResult lqRes = awsSQSClient.listQueues("testQ");
		assertNotNull(lqRes);
		// 2 queues expected
		assertEquals(2, lqRes.getQueueUrls().size());
		// Queue URLs from SQS should match what's in the object
		GetQueueUrlResult quRes = awsSQSClient.getQueueUrl(msgQImpl.getQueueName());
		assertNotNull(quRes);
		assertEquals(msgQImpl.getQueueUrl(), quRes.getQueueUrl());
		quRes = awsSQSClient.getQueueUrl(msgQImpl.getDeadLetterQueueName());
		assertEquals(msgQImpl.getDeadLetterQueueUrl(), quRes.getQueueUrl());
		// TODO: check that dead letter queue is setup as dead letter for queue
		
		// Verify that we can receive a msg once, then it's put on the dead letter queue
		SendMessageRequest smReq = new SendMessageRequest();
		smReq.setQueueUrl(msgQImpl.getQueueUrl());
		smReq.setDelaySeconds(1);
		smReq.setMessageBody(new String("theMessageBody"));
		awsSQSClient.sendMessage(smReq);
		Thread.sleep(1500L);
		ReceiveMessageRequest rmReqQ = new ReceiveMessageRequest();
		rmReqQ.setQueueUrl(msgQImpl.getQueueUrl());
		rmReqQ.setVisibilityTimeout(1);
		ReceiveMessageResult rmRes = awsSQSClient.receiveMessage(rmReqQ);
		assertNotNull(rmRes);
		List<Message> msgs = rmRes.getMessages();
		assertNotNull(msgs);
		assertEquals(1, msgs.size());
		assertEquals("theMessageBody", msgs.get(0).getBody());
		// Wait > 1 sec, msg should not be back on queue
		Thread.sleep(1500L);
		rmRes = awsSQSClient.receiveMessage(rmReqQ);
		assertNotNull(rmRes);
		msgs = rmRes.getMessages();
		assertNotNull(msgs);
		assertEquals(0, msgs.size());
		// Now it should be on the dead letter queue
		
	}

}
