package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.sqs.model.Message;

/**
 * Helper methods for messages
 * @author John
 *
 */
public class MessageUtils {
	
	private static final String CONCRETE_TYPE = "concreteType";

	public static class MessageBundle {
		private final Message message;
		private final ChangeMessage changeMessage;

		public MessageBundle(Message message, ChangeMessage changeMessage) {
			this.message = message;
			this.changeMessage = changeMessage;
		}

		public Message getMessage() {
			return message;
		}

		public ChangeMessage getChangeMessage() {
			return changeMessage;
		}
	}

	public static int SQS_MAX_REQUEST_SIZE = 10;

	@SuppressWarnings("unchecked")
	public static <T extends JSONEntity> T extractMessageBody(Message message, Class<T> clazz) {
		ValidateArgument.required(message, "message");
		try {
			JSONObject object = extractMessageBodyAsJSONObject(message);
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(object);
			T result;
			if (adapter.has(CONCRETE_TYPE)) {
				String concreteType = adapter.getString(CONCRETE_TYPE);
				result = (T) Class.forName(concreteType).newInstance();
			} else {
				result = clazz.newInstance();
			}
			result.initializeFromJSONObject(adapter);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Extract a message body as a JSON object. This handles the case where messages are forwarded from a topic.
	 * 
	 * @param message
	 * @return
	 * @throws JSONException 
	 */
	public static JSONObject extractMessageBodyAsJSONObject(Message message) throws JSONException {
		ValidateArgument.required(message, "message");
		JSONObject object = new JSONObject(message.getBody());
		if (object.has("TopicArn") && object.has("Message")) {
			return new JSONObject(object.getString("Message"));
		} else {
			return object;
		}
	}

	/**
	 * Extract a ChangeMessage from an Amazon Message
	 * @param message
	 * @return
	 */
	public static ChangeMessage extractMessageBody(Message message){
		return extractMessageBody(message, ChangeMessage.class);
	}
	
	/**
	 * Extract a list of change messages from a message.
	 * This will handle messages directly from a queue or forwarded from a topic.
	 * The change messages can either be written as a signle or a batch.
	 * 
	 * @param message
	 * @return
	 */
	public static List<ChangeMessage> extractChangeMessageBatch(Message message){
		try {
			JSONObject object = extractMessageBodyAsJSONObject(message);
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(object);
			if(object.has("list")){
				return  new ChangeMessages(adapter).getList();
			}else if (object.has("objectId")){
				return Arrays.asList(new ChangeMessage(adapter));
			}else{
				throw new IllegalArgumentException("Unknown message body: "+message.getBody());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Extract a ChangeMessage from an Amazon Message
	 * 
	 * @param message
	 * @return
	 */
	public static MessageBundle extractMessageBundle(Message message) {
		if (message == null)
			throw new IllegalArgumentException("Message cannot be null");
		ChangeMessage changeMessage = extractMessageBody(message);
		return new MessageBundle(message, changeMessage);
	}

	/**
	 * Extracts a UnsentMessageRange from an Amazon Message
	 */
	public static UnsentMessageRange extractUnsentMessageBody(Message message) {
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		try {
			JSONObject object = new JSONObject(message.getBody());
			JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(object);
			return new UnsentMessageRange(adapter);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * When a message is first published to a topic, then pushed to a queue, queue message body contains the entire topic message body.
	 * The topic message body then contains the original message.
	 * @param message
	 * @param messageId
	 * @param receiptHandle
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws JSONException 
	 */
	public static Message createTopicMessage(JSONEntity message, String topicArn, String messageId, String receiptHandle) throws JSONObjectAdapterException, JSONException{
		String messageJson = EntityFactory.createJSONStringForEntity(message);
		JSONObject jsonObj  = new JSONObject();
		jsonObj.put("MessageId", "d706461b-738e-42a2-8cfc-d0f50dc2d9e6");
		jsonObj.put("TopicArn", topicArn);
		jsonObj.put("Type", "Notification");
		jsonObj.put("Message", messageJson);
		String body = jsonObj.toString();
		return new Message().withBody(body).withMessageId(messageId).withReceiptHandle(receiptHandle);
	}
	
	/**
	 * Create an Amazon message from a ChangeMessage.  This is used for testing.
	 * @param message
	 * @return
	 */
	public static Message createMessage(JSONEntity message, String messageId, String receiptHandle){
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		try {
			Message result = new Message().withMessageId(messageId).withReceiptHandle(receiptHandle);
			result.setBody(EntityFactory.createJSONStringForEntity(message));
			return result;
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Creates an entity delete message.
	 */
	public static Message buildDeleteEntityMessage(String nodeId, String parentId, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.DELETE, nodeId, parentId, null, messageId, messageHandle);
	}

	/**
	 * Creates an Entity Create message.
	 */
	public static Message buildCreateEntityMessage(String nodeId, String parentId,
			String etag, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.CREATE, nodeId, parentId, etag, messageId, messageHandle);
	}

	/**
	 * Creates an entity Update message.
	 */
	public static Message buildUpdateEntityMessage(String nodeId, String parentId,
			String etag, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.UPDATE, nodeId, parentId, etag, messageId, messageHandle);
	}

	/**
	 * Creates an entity message.
	 */
	public static Message buildEntityMessage(ChangeType type, String nodeId, String parentId,
			String etag, String messageId, String messageHandle){
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(type);
		message.setObjectId(nodeId);
		message.setObjectType(ObjectType.ENTITY);
		return MessageUtils.createMessage(message, messageId, messageHandle);
	}
	
	/**
	 * Build a generic message.
	 * @param changeType
	 * @param objectId
	 * @param objectType
	 * @param parentId
	 * @param etag
	 * @param timestamp
	 * @return
	 */
	public static Message buildMessage(ChangeType changeType, String objectId, ObjectType objectType, String parentId, String etag, long timestamp){
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectId(objectId);
		message.setObjectType(objectType);
		message.setTimestamp(new Date(timestamp));
		return MessageUtils.createMessage(message, UUID.randomUUID().toString(), UUID.randomUUID().toString());
	}
	
	/**
	 * Build a generic message.
	 * @param changeType
	 * @param objectId
	 * @param objectType
	 * @param etag
	 * @return
	 */
	public static Message buildMessage(ChangeType changeType, String objectId, ObjectType objectType, String etag){
		return buildMessage(changeType, objectId, objectType, null, etag, System.currentTimeMillis());
	}

	/**
	 * Build a generic message.
	 * @param changeType
	 * @param objectId
	 * @param objectType
	 * @param etag
	 * @param timestamp
	 * @return
	 */
	public static Message buildMessage(ChangeType changeType, String objectId, ObjectType objectType, String etag, Long timestamp){
		return buildMessage(changeType, objectId, objectType, null, etag, timestamp);
	}


	
	/**
	 * Create a message with the passed JSONEntity as the body of the message.
	 * @param body
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static Message buildMessage(JSONEntity body) throws JSONObjectAdapterException{
		Message message = new Message();
		message.setBody(EntityFactory.createJSONStringForEntity(body));
		return message;
	}
	
	/**
	 * Create a message with the passed JSONEntity as the body of the message.
	 * @param body
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	public static Message buildMessage(AsynchronousJobStatus status) throws JSONObjectAdapterException{
		Message message = new Message();
		message.setBody(status.getJobId());
		return message;
	}

}
