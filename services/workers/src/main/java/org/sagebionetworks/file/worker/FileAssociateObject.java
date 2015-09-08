package org.sagebionetworks.file.worker;

import org.sagebionetworks.repo.model.file.FileHandleAssociateType;


/**
 * Describes a FileHandle associated object.
 *
 */
public class FileAssociateObject {
	
	String objectId;
	FileHandleAssociateType objectType;
	
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public FileHandleAssociateType getObjectType() {
		return objectType;
	}
	public void setObjectType(FileHandleAssociateType objectType) {
		this.objectType = objectType;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
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
		FileAssociateObject other = (FileAssociateObject) obj;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType != other.objectType)
			return false;
		return true;
	}


}
