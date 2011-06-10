package org.sagebionetworks.web.shared.users;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AclEntry implements IsSerializable {

	private String aclEntryId;
	private AclPrincipal principal;
	private List<AclAccessType> accessTypes;
	private boolean isOwner;
	
	public AclEntry() {
		
	}

	public AclEntry(String aclEntryId, AclPrincipal principal, List<AclAccessType> accessTypes, boolean isOwner) {
		super();
		this.aclEntryId = aclEntryId;
		this.principal = principal;
		this.accessTypes = accessTypes;
		this.isOwner = isOwner;
	}

	public String getAclEntryId() {
		return aclEntryId;
	}

	public void setAclEntryId(String aclEntryId) {
		this.aclEntryId = aclEntryId;
	}

	public AclPrincipal getPrincipal() {
		return principal;
	}

	public void setPrincipal(AclPrincipal principal) {
		this.principal = principal;
	}

	public List<AclAccessType> getAccessTypes() {
		return accessTypes;
	}

	public void setAccessTypes(List<AclAccessType> accessTypes) {
		this.accessTypes = accessTypes;
	}
	
	public boolean isOwner() {
		return isOwner;
	}

	public void setOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessTypes == null) ? 0 : accessTypes.hashCode());
		result = prime * result
				+ ((aclEntryId == null) ? 0 : aclEntryId.hashCode());
		result = prime * result + (isOwner ? 1231 : 1237);
		result = prime * result
				+ ((principal == null) ? 0 : principal.hashCode());
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
		AclEntry other = (AclEntry) obj;
		if (accessTypes == null) {
			if (other.accessTypes != null)
				return false;
		} else if (!accessTypes.equals(other.accessTypes))
			return false;
		if (aclEntryId == null) {
			if (other.aclEntryId != null)
				return false;
		} else if (!aclEntryId.equals(other.aclEntryId))
			return false;
		if (isOwner != other.isOwner)
			return false;
		if (principal == null) {
			if (other.principal != null)
				return false;
		} else if (!principal.equals(other.principal))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AclEntry [aclEntryId=" + aclEntryId + ", principal="
				+ principal + ", accessTypes=" + accessTypes + ", isOwner="
				+ isOwner + "]";
	}

}
