package org.sagebionetworks.repo.model;

import java.util.Set;

/**
 * Note that equals and hashcode are defined on groupId only
 * This is because these objects only go together in a Set 
 * belonging to an AccessControlList, which has only one
 * object for each group.
 */
public class ResourceAccess {
	private String id;
	private String userGroupId;
	private Set<AuthorizationConstants.ACCESS_TYPE> accessType;
	
	@Override
	public String toString() {
		return "ResourceAccess [id=" + id + ", userGroupId=" + userGroupId
				+ ", accessType=" + accessType + "]";
	}
	
	/**
	 * @return the userGroupId
	 */
	public String getUserGroupId() {
		return userGroupId;
	}
	/**
	 * @param userGroupId the userGroupId to set
	 */
	public void setUserGroupId(String userGroupId) {
		this.userGroupId = userGroupId;
	}
	/**
	 * @return the accessType
	 */
	public Set<AuthorizationConstants.ACCESS_TYPE> getAccessType() {
		return accessType;
	}
	/**
	 * @param accessType the accessType to set
	 */
	public void setAccessType(Set<AuthorizationConstants.ACCESS_TYPE> accessType) {
		this.accessType = accessType;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((userGroupId == null) ? 0 : userGroupId.hashCode());
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
		ResourceAccess other = (ResourceAccess) obj;
		if (accessType == null) {
			if (other.accessType != null)
				return false;
		} else if (!accessType.equals(other.accessType))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (userGroupId == null) {
			if (other.userGroupId != null)
				return false;
		} else if (!userGroupId.equals(other.userGroupId))
			return false;
		return true;
	}
	
}
