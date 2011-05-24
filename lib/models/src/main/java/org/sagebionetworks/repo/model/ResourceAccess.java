package org.sagebionetworks.repo.model;

import java.util.Set;

/**
 * Note that equals and hashcode are defined on groupId only
 * This is because these objects only go together in a Set 
 * belonging to an AccessControlList, which has only one
 * object for each group.
 */
public class ResourceAccess {
	private String userGroupId;
	private Set<AuthorizationConstants.ACCESS_TYPE> accessType;
	
	public String toString() {return userGroupId+" "+accessType;}
	
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
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((userGroupId == null) ? 0 : userGroupId.hashCode());
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ResourceAccess))
			return false;
		ResourceAccess other = (ResourceAccess) obj;
		if (userGroupId == null) {
			if (other.userGroupId != null)
				return false;
		} else if (!userGroupId.equals(other.userGroupId))
			return false;
		return true;
	}
	
	
}
