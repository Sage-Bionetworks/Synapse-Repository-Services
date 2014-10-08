package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.repo.model.ObjectType;

/**
 * The change key is a composite of the object type and the object id.
 * 
 * @author John
 *
 */
class ChangeMessageKey {
	
	private ObjectType type;
	private String objectId;
	private Boolean isModification;
	
	public ChangeMessageKey(ChangeMessage message) {
		if(message == null) throw new IllegalArgumentException("ChangeMessage cannot be null");
		if(message.getObjectId() == null) throw new IllegalArgumentException("ChangeMessage.getObjectId() cannot be null");
		if(message.getObjectType()== null) throw new IllegalArgumentException("ChangeMessage.getObjectType() cannot be null");
		this.type = message.getObjectType();
		this.objectId = message.getObjectId();
		this.isModification = message.getIsModification();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isModification == null) ? 0 : isModification.hashCode());
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
		ChangeMessageKey other = (ChangeMessageKey) obj;
		if (isModification == null) {
			if (other.isModification != null)
				return false;
		} else if (!isModification.equals(other.isModification))
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
		return "ChangeMessageKey [type=" + type + ", objectId=" + objectId + ", isModification=" + isModification + "]";
	}
}