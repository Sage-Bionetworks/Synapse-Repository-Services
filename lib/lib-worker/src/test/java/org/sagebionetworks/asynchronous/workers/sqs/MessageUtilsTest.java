package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.amazonaws.services.sqs.model.Message;

public class MessageUtilsTest {
	
	@Test
	public void testExtractQueueMessageBody() throws JSONObjectAdapterException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectId("456");
		message.setObjectId("synABC");
		// Set the message
		Message awsMessage = MessageUtils.createMessage(message, "id", "handle");
		
		// Extract it
		List<ChangeMessage> result = MessageUtils.extractChangeMessageBatch(awsMessage);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(message, result.get(0));
	}
	
	@Test
	public void testExtractQueueMessageBodyBatch() throws JSONObjectAdapterException{
		//one
		ChangeMessage one = new ChangeMessage();
		one.setChangeType(ChangeType.DELETE);
		one.setObjectId("456");
		one.setObjectId("synABC");
		//two
		ChangeMessage two = new ChangeMessage();
		two.setChangeType(ChangeType.DELETE);
		two.setObjectId("789");
		two.setObjectId("synXYZ");
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(one, two));
		// Set the message
		Message awsMessage = MessageUtils.buildMessage(messages);
		
		// Extract it
		List<ChangeMessage> result = MessageUtils.extractChangeMessageBatch(awsMessage);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(one, result.get(0));
		assertEquals(two, result.get(1));
	}
	
	@Test
	public void testExtractTopicMessageBody() throws JSONObjectAdapterException, JSONException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectId("456");
		message.setObjectId("synABC");
		// Set the message
		Message awsMessage = MessageUtils.createTopicMessage(message, "topic:arn","id", "handle");
		
		// Extract it
		List<ChangeMessage> result = MessageUtils.extractChangeMessageBatch(awsMessage);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(message, result.get(0));
	}
	
	@Test
	public void testExtractChangeMessageBatchTopic() throws JSONObjectAdapterException, JSONException{
		//one
		ChangeMessage one = new ChangeMessage();
		one.setChangeType(ChangeType.DELETE);
		one.setObjectId("456");
		one.setObjectId("synABC");
		//two
		ChangeMessage two = new ChangeMessage();
		two.setChangeType(ChangeType.DELETE);
		two.setObjectId("789");
		two.setObjectId("synXYZ");
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(one, two));
		// Set the message
		Message awsMessage = MessageUtils.createTopicMessage(messages, "topic:arn","id", "handle");
		
		// Extract it
		List<ChangeMessage> result = MessageUtils.extractChangeMessageBatch(awsMessage);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(one, result.get(0));
		assertEquals(two, result.get(1));
	}
	

}
