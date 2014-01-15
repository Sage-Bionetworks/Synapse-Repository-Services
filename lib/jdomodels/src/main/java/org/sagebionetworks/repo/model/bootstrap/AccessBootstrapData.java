package org.sagebionetworks.repo.model.bootstrap;

import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

/**
 * Metadata used to create a resource access.
 * 
 * Note:  'group' and 'accessTypeList' are set via Spring (specified in an XML file)
 * Later, 'groupId' is added.
 * 
 * @author jmhill
 *
 */
public class AccessBootstrapData {
	
	private BOOTSTRAP_PRINCIPAL group;
	private List<ACCESS_TYPE> accessTypeList;
	
	public BOOTSTRAP_PRINCIPAL getGroup() {
		return group;
	}
	public void setGroup(BOOTSTRAP_PRINCIPAL group) {
		this.group = group;
	}
	@Override
	public String toString() {
		return "AccessBootstrapData [group=" + group + ", accessTypeList="
				+ accessTypeList + "]";
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
	

