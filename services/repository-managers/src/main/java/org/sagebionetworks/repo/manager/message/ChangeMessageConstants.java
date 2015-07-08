package org.sagebionetworks.repo.manager.message;

import java.util.Arrays;
import java.util.Date;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class ChangeMessageConstants {
	
	/**
	 * The approximate size of a single change messages written to JSON in bytes.
	 */
	public static final int APPROXIMATE_SIZE_OF_CHANGE_MESSAGES_JSON_BYTES;
	
	static{
		// Calculate the size in bytes for a change messages string.
		ChangeMessage change = new ChangeMessage();
		change.setChangeNumber(Long.MAX_VALUE);
		change.setChangeType(ChangeType.UPDATE);
		change.setObjectEtag("a4fc5142-6a33-4255-bb47-6dbb0e3f055c");
		change.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		change.setObjectId(""+Long.MAX_VALUE);
		change.setParentId(""+Long.MAX_VALUE);
		change.setTimestamp(new Date(292278993));
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(change));
		String json;
		try {
			json = EntityFactory.createJSONStringForEntity(messages);
			APPROXIMATE_SIZE_OF_CHANGE_MESSAGES_JSON_BYTES = json.getBytes("UTF-8").length;
		} catch (Exception e) {
			// should never occur
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * See: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/APIReference/API_SendMessage.html
	 * The limit is 256KB (262,144 bytes) according to the above docs.  We are using 210K to ensure we stay under the limit.
	 */
	public static final int MAX_SQS_MESSAGES_SIZE_BYTES = 210*1000;
	
	/**
	 * The calculated maximum number of change messages that can be written to a single Amazon SQS messages body. 
	 */
	public static final int MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE = MAX_SQS_MESSAGES_SIZE_BYTES/APPROXIMATE_SIZE_OF_CHANGE_MESSAGES_JSON_BYTES;

}
