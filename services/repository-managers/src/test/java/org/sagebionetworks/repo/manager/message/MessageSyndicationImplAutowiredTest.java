package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageSyndicationImplAutowiredTest {
	
	public static final long MAX_WAIT = 10*1000; //ten seconds
	
	@Autowired
	private MessageSyndication messageSyndication;
	
	@Autowired
	private SqsClient awsSQSClient;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	private String queueName = StackConfigurationSingleton.singleton().getStack()+"-"+StackConfigurationSingleton.singleton().getStackInstance()+"-test-syndication";
	private String queueUrl;
	
	@BeforeEach
	public void before(){
		// Create the queue if it does not exist
		CreateQueueResponse cqr = awsSQSClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
		queueUrl = cqr.queueUrl();
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
	
	
	@AfterEach
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
		ReceiveMessageResponse result = null;
		do{
			result = awsSQSClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(10).visibilityTimeout(100).build());
			List<Message> list = result.messages();
			if(list.size() > 0){
				List<DeleteMessageBatchRequestEntry> batch = new LinkedList<DeleteMessageBatchRequestEntry>();
				for(int i=0; i< list.size(); i++){
					Message message = list.get(i);
					// Delete all of them.
					batch.add(DeleteMessageBatchRequestEntry.builder().id(""+i).receiptHandle(message.receiptHandle()).build());
				}
				awsSQSClient.deleteMessageBatch(DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(batch).build());
			}
			System.out.println("Deleted "+list.size()+" messages");
		}while(result.messages().size() > 0);
	}

}
