package org.sagebionetworks.repo.manager.message;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:aws-topic-publisher.spb.xml" })
public class MessageSyndicationImplTest {
	
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
		// Make sure the queue starts empty.
		emptyQueue();
	}
	
	@Test
	public void testRebroadcastAllChangeMessagesToQueue(){
		// Start with no change messages
		changeDAO.deleteAllChanges();
		// Create a bunch of messages
	}
	
	@After
	public void after(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	public void createChangeMessages(int count, ChangeType type){
		for(int i=0; i<count; i++){
			ChangeMessage message = new ChangeMessage();
			message.setChangeType(type);
			message.setObjectId(""+i);
			changeDAO.replaceChange(message);
		}
	}
	
	/**
	 * Helper to empty the message queue
	 */
	public void emptyQueue(){
		ReceiveMessageResult result = null;
		do{
			result = awsSQSClient.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10).withVisibilityTimeout(100));
			List<Message> list = result.getMessages();
			for(Message message: list){
				// Delete all of them.
				awsSQSClient.deleteMessage(new DeleteMessageRequest(queueUrl, message.getReceiptHandle()));
			}
			System.out.println("Deleted "+list.size()+" messages");
		}while(result.getMessages().size() > 0);
	}

}
