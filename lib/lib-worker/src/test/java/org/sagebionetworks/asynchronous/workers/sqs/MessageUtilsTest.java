package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.services.sqs.model.Message;

public class MessageUtilsTest {
	
	@Test
	public void testExtractMessageBody() throws JSONObjectAdapterException{
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

}
