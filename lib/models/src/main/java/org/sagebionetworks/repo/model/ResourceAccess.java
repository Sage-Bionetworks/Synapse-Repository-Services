package org.sagebionetworks.repo.model;

import java.util.Set;

/**
 * Note that equals and hashcode are defined on groupId only This is because
 * these objects only go together in a Set belonging to an AccessControlList,
 * which has only one object for each group.
 */
public class ResourceAccess {
	private String groupName;
	private Set<AuthorizationConstants.ACCESS_TYPE> accessType;

	/**
	 * @return the accessType
	 */
	public Set<AuthorizationConstants.ACCESS_TYPE> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType
	 *            the accessType to set
	 */
	public void setAccessType(Set<AuthorizationConstants.ACCESS_TYPE> accessType) {
		this.accessType = accessType;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessType == null) ? 0 : accessType.hashCode());
		result = prime * result
				+ ((groupName == null) ? 0 : groupName.hashCode());
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
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceAccess [groupName=" + groupName + ", accessType="
				+ accessType + "]";
	}

}
