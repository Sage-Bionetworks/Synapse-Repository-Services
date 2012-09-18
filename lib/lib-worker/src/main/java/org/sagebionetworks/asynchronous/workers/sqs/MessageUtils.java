package org.sagebionetworks.asynchronous.workers.sqs;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.amazonaws.services.sqs.model.Message;

/**
 * Helper methods for messages
 * @author John
 *
 */
public class MessageUtils {
	
	/**
	 * Extract a ChangeMessage from an Amazon Message
	 * @param message
	 * @return
	 */
	public static ChangeMessage extractMessageBody(Message message){
		if(message == null) throw new IllegalArgumentException("Message cannot be null");
		try {
			JSONObject object = new JSONObject(message.getBody());
			if(object.has("objectId")){
				// This is a message pushed directly to a queue
				JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(object);
				return new ChangeMessage(adapter);
			}if(object.has("TopicArn") && object.has("Message") ){
				// This is a message that was pushed to a topic then forwarded to a queue.
				JSONObject innerObject = new JSONObject(object.getString("Message"));
				JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(innerObject);
				return new ChangeMessage(adapter);
			}else{
				throw new IllegalArgumentException("Unknown message type: "+message.getBody());
			}
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
	public static Message createTopicMessage(ChangeMessage message, String topicArn, String messageId, String receiptHandle) throws JSONObjectAdapterException, JSONException{
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
	public static Message createMessage(ChangeMessage message, String messageId, String receiptHandle){
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
	 * Create an entity delete message.
	 * @param id
	 * @return
	 */
	public static Message buildDeleteEntityMessage(String nodeId, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.DELETE, nodeId, null, messageId, messageHandle);
	}
	
	/**
	 * Create an Entity Create message
	 * @param id
	 * @return
	 */
	public static Message buildCreateEntityMessage(String nodeId, String etag, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.CREATE, nodeId, etag, messageId, messageHandle);
	}
	
	/**
	 * Create an entity Update message.
	 * @param nodeId
	 * @param etag
	 * @param messageId
	 * @param messageHandle
	 * @return
	 */
	public static Message buildUpdateEntityMessage(String nodeId, String etag, String messageId, String messageHandle){
		return buildEntityMessage(ChangeType.UPDATE, nodeId, etag, messageId, messageHandle);
	}
	
	/**
	 * Create an entity message
	 * @param type
	 * @param nodeId
	 * @param etag
	 * @param messageId
	 * @param messageHandle
	 * @return
	 */
	public static Message buildEntityMessage(ChangeType type, String nodeId, String etag, String messageId, String messageHandle){
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(type);
		message.setObjectEtag(etag);
		message.setObjectId(nodeId);
		message.setObjectType(ObjectType.ENTITY);
		return MessageUtils.createMessage(message, messageId, messageHandle);
	}

}
