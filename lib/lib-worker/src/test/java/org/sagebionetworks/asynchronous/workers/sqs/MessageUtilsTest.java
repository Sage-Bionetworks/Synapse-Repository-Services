package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.amazonaws.services.sqs.model.Message;

public class MessageUtilsTest {
	
	@Test
	public void testExtractQueueMessageBody() throws JSONObjectAdapterException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectEtag("123");
		message.setObjectId("456");
		message.setObjectId("synABC");
		// Set the message
		Message awsMessage = MessageUtils.createMessage(message, "id", "handle");
		
		// Extract it
		ChangeMessage result = MessageUtils.extractMessageBody(awsMessage);
		assertEquals(message, result);
	}
	
	@Test
	public void testExtractTopicMessageBody() throws JSONObjectAdapterException, JSONException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.DELETE);
		message.setObjectEtag("123");
		message.setObjectId("456");
		message.setObjectId("synABC");
		// Set the message
		Message awsMessage = MessageUtils.createTopicMessage(message, "topic:arn","id", "handle");
		
		// Extract it
		ChangeMessage result = MessageUtils.extractMessageBody(awsMessage);
		assertEquals(message, result);
	}
	
	@Test
	public void testListSplitting() throws Exception {
		List<Integer> batch = new ArrayList<Integer>();
		for (int i = 0; i < 100; i++) {
			batch.add(i);
			List<List<Integer>> miniBatches = MessageUtils.splitListIntoTens(batch);
			List<Integer> reBatched = new ArrayList<Integer>();
			for (int j = 0; j < miniBatches.size(); j++) {
				reBatched.addAll(miniBatches.get(j));
			}
			Assert.assertEquals(batch, reBatched);
		}
	}

}
