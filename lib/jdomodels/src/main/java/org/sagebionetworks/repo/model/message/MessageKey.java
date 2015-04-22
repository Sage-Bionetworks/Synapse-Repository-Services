package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * The change key is a composite of the object type and the object id.
 * 
 * @author John
 *
 */
class MessageKey {
	
	private ObjectType type;
	private String objectId;
	private Class<? extends Message> messageType;
	
	public MessageKey(Message message) {
		if (message == null)
			throw new IllegalArgumentException("Message cannot be null");
		if (message.getObjectId() == null)
			throw new IllegalArgumentException("Message.getObjectId() cannot be null");
		if (message.getObjectType() == null)
			throw new IllegalArgumentException("Message.getObjectType() cannot be null");
		this.type = message.getObjectType();
		this.objectId = message.getObjectId();
		this.messageType = message.getClass();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageType == null) ? 0 : messageType.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MessageKey other = (MessageKey) obj;
		if (messageType == null) {
			if (other.messageType != null)
				return false;
		} else if (!messageType.equals(other.messageType))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MessageKey [type=" + type + ", objectId=" + objectId + ", messageType=" + messageType + "]";
	}
}