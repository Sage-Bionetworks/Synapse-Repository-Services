package org.sagebionetworks.repo.model;

public class AccessRequirementInfoForUpdate {

	Long accessRequirementId;
	Long currentVersion;
	String etag;
	String concreteType;
	ACCESS_TYPE accessType;
	public Long getAccessRequirementId() {
		return accessRequirementId;
	}
	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}
	public Long getCurrentVersion() {
		return currentVersion;
	}
	public void setCurrentVersion(Long currentVersion) {
		this.currentVersion = currentVersion;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public String getConcreteType() {
		return concreteType;
	}
	public void setConcreteType(String concreteType) {
		this.concreteType = concreteType;
	}
	public ACCESS_TYPE getAccessType() {
		return accessType;
	}
	public void setAccessType(ACCESS_TYPE accessType) {
		this.accessType = accessType;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRequirementId == null) ? 0 : accessRequirementId.hashCode());
		result = prime * result + ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result + ((concreteType == null) ? 0 : concreteType.hashCode());
		result = prime * result + ((currentVersion == null) ? 0 : currentVersion.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
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
		AccessRequirementInfoForUpdate other = (AccessRequirementInfoForUpdate) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (accessType != other.accessType)
			return false;
		if (concreteType == null) {
			if (other.concreteType != null)
				return false;
		} else if (!concreteType.equals(other.concreteType))
			return false;
		if (currentVersion == null) {
			if (other.currentVersion != null)
				return false;
		} else if (!currentVersion.equals(other.currentVersion))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "AccessRequirementInfoForUpdate [accessRequirementId=" + accessRequirementId + ", currentVersion="
				+ currentVersion + ", etag=" + etag + ", concreteType=" + concreteType + ", accessType=" + accessType
				+ "]";
	}
}
