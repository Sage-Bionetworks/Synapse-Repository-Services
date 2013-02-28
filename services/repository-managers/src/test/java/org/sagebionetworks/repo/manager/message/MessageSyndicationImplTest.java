package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:aws-topic-publisher.spb.xml" })
public class MessageSyndicationImplTest {
	
	public static final long MAX_WAIT = 10*1000; //ten seconds
	
	@Autowired
	MessageSyndication messageSyndication;
	
	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	private String queueName = StackConfiguration.getStack()+"-"+StackConfiguration.getStackInstance()+"-test-syndication";
	private String queueUrl;
	
	@Before
	public void before(){
		// Create the queue if it does not exist
		CreateQueueResult cqr = awsSQSClient.createQueue(new CreateQueueRequest(queueName));
		queueUrl = cqr.getQueueUrl();
		System.out.println("Queue Name: "+queueName);
		System.out.println("Queue URL: "+queueUrl);

	}
	
	@Test
	public void testRebroadcastAllChangeMessagesToQueue() throws InterruptedException{
		// Make sure the queue starts empty.
		emptyQueue();
		assertEquals(0, getQueueMessageCount());
		// Start with no change messages
		changeDAO.deleteAllChanges();
		// Create a bunch of messages
		long toCreate = 15l;
		createChangeMessages(toCreate, ObjectType.ENTITY);
		// Now push all of these to the queue
		long result = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, 0l, Long.MAX_VALUE);
		assertEquals(toCreate, result);
		waitForMessageCount(toCreate);
	}
	
	@After
	public void after(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	/**
	 * Create the given number of messages.
	 * 
	 * @param count
	 * @param type
	 */
	public void createChangeMessages(long count, ObjectType type){
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setObjectType(type);
			message.setObjectId(""+i);
			// Use all types
			message.setChangeType(ChangeType.values()[i%ChangeType.values().length]);
			message.setObjectEtag("etag"+i);
			changeDAO.replaceChange(message);
		}
	}
	
	/**
	 * Wait for a given message count;
	 * @param expectedCount
	 * @throws InterruptedException
	 */
	public void waitForMessageCount(long expectedCount) throws InterruptedException{
		long start = System.currentTimeMillis();
		long count;
		do{
			count = getQueueMessageCount();
			System.out.println("Waiting for expected message count...");
			Thread.sleep(1000);
			assertTrue("Timed out waiting for the expected message count", System.currentTimeMillis()-start < MAX_WAIT);
		}while(count != expectedCount);
	}
	
	public long getQueueMessageCount(){
		GetQueueAttributesResult result = awsSQSClient.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames("ApproximateNumberOfMessages"));
		return Long.parseLong(result.getAttributes().get("ApproximateNumberOfMessages"));
	}
	
	/**
	 * Helper to empty the message queue
	 */
	public void emptyQueue(){
		ReceiveMessageResult result = null;
		do{
			result = awsSQSClient.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10).withVisibilityTimeout(100));
			List<Message> list = result.getMessages();
			if(list.size() > 0){
				List<DeleteMessageBatchRequestEntry> batch = new LinkedList<DeleteMessageBatchRequestEntry>();
				for(int i=0; i< list.size(); i++){
					Message message = list.get(i);
					// Delete all of them.
					batch.add(new DeleteMessageBatchRequestEntry(""+i, message.getReceiptHandle()));
				}
				awsSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
			}
			System.out.println("Deleted "+list.size()+" messages");
		}while(result.getMessages().size() > 0);
	}

}
