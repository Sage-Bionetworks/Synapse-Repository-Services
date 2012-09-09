package org.sagebionetworks.asynchronous.workers.sqs.search;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:sqs-manager-spb.xml" })
public class SearchMessageQueueAutowireTest {
	
	public static final long MAX_WAIT_MS = 10*1000; // 10 sec
	
	@Autowired
	SearchMessageQueue searchMessageQueue;
	@Autowired
	AmazonSQSClient awsSQSClient;
	
	@Autowired
	AmazonSNSClient awsSNSClient;
	
	@Test
	public void testInitialze(){
		assertNotNull(searchMessageQueue);
		assertNotNull(searchMessageQueue.getQueueName());
		assertNotNull(searchMessageQueue.getQueueUrl());
	}
	
	@Test
	public void testPublishTopicReveiveQueue() throws JSONObjectAdapterException, InterruptedException, JSONException{
		// Test that we can publish a message to the topic an then receive it on the queue
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectId("abc123");
		message.setObjectType(ObjectType.ENTITY);
		String json = EntityFactory.createJSONStringForEntity(message);
		String topicArn = searchMessageQueue.getTopicArn();
		// publish to the topic
		PublishResult pubResult = awsSNSClient.publish(new PublishRequest(topicArn, json));
		Thread.sleep(1000);
		ChangeMessage clone = waitForMessage(pubResult.getMessageId());
		assertEquals(message, clone);
	}
	
	/**
	 * Helper to wait for a message to appear on the queue.
	 * @param messageId
	 * @return
	 * @throws InterruptedException 
	 * @throws JSONException 
	 * @throws JSONObjectAdapterException 
	 */
	public ChangeMessage waitForMessage(String messageId) throws InterruptedException, JSONException, JSONObjectAdapterException{
		System.out.println("Waiting for messsageId: "+messageId);
		long start = System.currentTimeMillis();
		while(true){
			long elapse = System.currentTimeMillis()-start;
			if(elapse > MAX_WAIT_MS) throw new IllegalStateException("Timed out waiting for a message to appear on the queue: messageId:"+messageId);
			// Now fetch the message from the queue
			ReceiveMessageResult results = awsSQSClient.receiveMessage(new ReceiveMessageRequest(searchMessageQueue.getQueueUrl()).withVisibilityTimeout(1));
			// Scan the results
			for(Message message: results.getMessages()){
				System.out.println(message);
				JSONObject object = new JSONObject(message.getBody());
				String thisMessageId = object.getString("MessageId");
				if(messageId.equals(thisMessageId)){
					// Parse the body
					ChangeMessage clone = EntityFactory.createEntityFromJSONString(object.getString("Message"), ChangeMessage.class);
					// Delete this message from the queue.
					awsSQSClient.deleteMessage(new DeleteMessageRequest(searchMessageQueue.getQueueUrl(), message.getReceiptHandle()));
					return clone;
				}
			}
			Thread.sleep(1000);
		}

	}

}
