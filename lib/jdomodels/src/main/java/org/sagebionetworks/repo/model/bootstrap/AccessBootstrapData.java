package org.sagebionetworks.repo.model.bootstrap;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;

/**
 * Metadata used to create a resource access.
 * @author jmhill
 *
 */
public class AccessBootstrapData {
	
	private DEFAULT_GROUPS group;
	private List<ACCESS_TYPE> accessTypeList;
	
	public DEFAULT_GROUPS getGroup() {
		return group;
	}
	public void setGroup(DEFAULT_GROUPS group) {
		this.group = group;
	}
	public List<ACCESS_TYPE> getAccessTypeList() {
		return accessTypeList;
	}
	public void setAccessTypeList(List<ACCESS_TYPE> accessTypeList) {
		this.accessTypeList = accessTypeList;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessTypeList == null) ? 0 : accessTypeList.hashCode());
		result = prime * result + ((group == null) ? 0 : group.hashCode());
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
		AccessBootstrapData other = (AccessBootstrapData) obj;
		if (accessTypeList == null) {
			if (other.accessTypeList != null)
				return false;
		} else if (!accessTypeList.equals(other.accessTypeList))
			return false;
		if (group != other.group)
			return false;
		return true;
	}
	
}
	

