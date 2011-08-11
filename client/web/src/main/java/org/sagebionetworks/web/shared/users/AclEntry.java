package org.sagebionetworks.web.shared.users;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class AclEntry implements IsSerializable {

	private AclPrincipal principal;
	private List<AclAccessType> accessTypes;
	private boolean isOwner;
	
	public AclEntry() {
		
	}

	public AclEntry(AclPrincipal principal, List<AclAccessType> accessTypes, boolean isOwner) {
		super();
		this.principal = principal;
		this.accessTypes = accessTypes;
		this.isOwner = isOwner;
	}

	/**
	 * The identifier used to key this entry
	 * @return
	 */
	public String getPrincipalId() {
		return principal.getName();
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
		return "AclEntry [principal=" + principal + ", accessTypes="
				+ accessTypes + ", isOwner=" + isOwner + "]";
	}
}
