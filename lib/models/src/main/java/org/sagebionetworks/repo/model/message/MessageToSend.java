package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;

/**
 * Change message data to send after a commit.
 *
 */
public class MessageToSend {

	String objectId;
	Long objectVersion;
	ObjectType objectType;
	ChangeType changeType;
	Long userId;
	
	public MessageToSend withObservableEntity(ObservableEntity object) {
		this.objectId = object.getIdString();
		this.objectType = object.getObjectType();
		return this;
	}
	
	public String getObjectId() {
		return objectId;
	}
	public MessageToSend withObjectId(String objectId) {
		this.objectId = objectId;
		return this;
	}
	public ObjectType getObjectType() {
		return objectType;
	}
	public MessageToSend withObjectType(ObjectType objectType) {
		this.objectType = objectType;
		return this;
	}
	public ChangeType getChangeType() {
		return changeType;
	}
	public MessageToSend withChangeType(ChangeType changeType) {
		this.changeType = changeType;
		return this;
	}
	public Long getUserId() {
		return userId;
	}
	public MessageToSend withUserId(Long userId) {
		this.userId = userId;
		return this;
	}
	
	public Long getObjectVersion() {
		return objectVersion;
	}

	public MessageToSend withObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		MessageToSend other = (MessageToSend) obj;
		if (changeType != other.changeType)
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType != other.objectType)
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "MessageToSend [objectId=" + objectId + ", objectType=" + objectType + ", changeType="
				+ changeType + ", userId=" + userId + "]";
	}

	/**
	 * Build a change message from this object.
	 * @return
	 */
	public ChangeMessage buildChangeMessage() {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(changeType);
		message.setObjectType(objectType);
		message.setObjectId(objectId);
		message.setObjectVersion(objectVersion);
		message.setUserId(userId);
		return message;
	}
	
	
}
