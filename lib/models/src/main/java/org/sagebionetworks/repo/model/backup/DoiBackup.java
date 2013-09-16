package org.sagebionetworks.repo.model.backup;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;

public class DoiBackup {

	private Long id;
	private String eTag;
	private DoiStatus doiStatus;
	private Long objectId;
	@Deprecated
	private DoiObjectType doiObjectType;
	private ObjectType objectType;
	private Long objectVersion;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp updatedOn;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public DoiStatus getDoiStatus() {
		return doiStatus;
	}
	public void setDoiStatus(DoiStatus doiStatus) {
		this.doiStatus = doiStatus;
	}
	public Long getObjectId() {
		return objectId;
	}
	public void setObjectId(Long objectId) {
		this.objectId = objectId;
	}
	public DoiObjectType getDoiObjectType() {
		return doiObjectType;
	}
	public void setDoiObjectType(DoiObjectType doiObjectType) {
		this.doiObjectType = doiObjectType;
	}
	public ObjectType getObjectType() {
		return objectType;
	}
	public void setObjectType(ObjectType objectType) {
		this.objectType = objectType;
	}
	public Long getObjectVersion() {
		return objectVersion;
	}
	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
	}
	public Long getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}
	public Timestamp getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}
	public Timestamp getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((doiObjectType == null) ? 0 : doiObjectType.hashCode());
		result = prime * result
				+ ((doiStatus == null) ? 0 : doiStatus.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((objectType == null) ? 0 : objectType.hashCode());
		result = prime * result
				+ ((objectVersion == null) ? 0 : objectVersion.hashCode());
		result = prime * result
				+ ((updatedOn == null) ? 0 : updatedOn.hashCode());
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
		DoiBackup other = (DoiBackup) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (doiObjectType != other.doiObjectType)
			return false;
		if (doiStatus != other.doiStatus)
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (objectType != other.objectType)
			return false;
		if (objectVersion == null) {
			if (other.objectVersion != null)
				return false;
		} else if (!objectVersion.equals(other.objectVersion))
			return false;
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DoiBackup [id=" + id + ", eTag=" + eTag + ", doiStatus="
				+ doiStatus + ", objectId=" + objectId + ", doiObjectType="
				+ doiObjectType + ", objectType=" + objectType
				+ ", objectVersion=" + objectVersion + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", updatedOn="
				+ updatedOn + "]";
	}
}
