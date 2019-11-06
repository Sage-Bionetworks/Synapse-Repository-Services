package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.PublishResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageSyndicationImplAutowiredTest {
	
	public static final long MAX_WAIT = 10*1000; //ten seconds
	
	@Autowired
	MessageSyndication messageSyndication;
	
	@Autowired
	AmazonSQS awsSQSClient;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	private String queueName = StackConfigurationSingleton.singleton().getStack()+"-"+StackConfigurationSingleton.singleton().getStackInstance()+"-test-syndication";
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
		// Start with no change messages
		changeDAO.deleteAllChanges();
		// Create a bunch of messages
		long toCreate = 15l;
		List<ChangeMessage> created = createChangeMessages(toCreate, ObjectType.ENTITY);
		ChangeMessages allMessages = messageSyndication.listChanges(0l,  ObjectType.ENTITY,  Long.MAX_VALUE);
		assertNotNull(allMessages);
		assertNotNull(allMessages.getList());
		assertEquals(toCreate, allMessages.getList().size());
		// Now push all of these to the queue
		PublishResults results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, 0l, Long.MAX_VALUE);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(toCreate, results.getList().size());
		
		// Now send a single message.
		ChangeMessage toTest = created.get(13);
		// Now push all of these to the queue
		results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, toTest.getChangeNumber(), 1l);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(1, results.getList().size());
		// Validate that the correct message was sent
		PublishResult pr = results.getList().get(0);
		assertNotNull(pr);
		assertEquals(toTest.getChangeNumber(), pr.getChangeNumber());
		
		// Test a page
		ChangeMessage start = created.get(2);
		Long limit = 11l;
		// Now push all of these to the queue
		results = messageSyndication.rebroadcastChangeMessagesToQueue(queueName, ObjectType.ENTITY, start.getChangeNumber(), limit);
		
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(limit.intValue(), results.getList().size());
		// validate the results
		assertEquals(start.getChangeNumber(), results.getList().get(0).getChangeNumber());
		assertEquals(created.get(2+11-1).getChangeNumber(), results.getList().get(limit.intValue()-1).getChangeNumber());
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
	public List<ChangeMessage> createChangeMessages(long count, ObjectType type){
		List<ChangeMessage> results = new ArrayList<ChangeMessage>();
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setObjectType(type);
			message.setObjectId(""+i);
			// Use all types
			message.setChangeType(ChangeType.values()[i%ChangeType.values().length]);
			results.add(changeDAO.replaceChange(message));
		}
		return results;
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
