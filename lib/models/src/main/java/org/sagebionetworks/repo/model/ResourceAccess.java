package org.sagebionetworks.repo.model;

import java.util.Collection;

public class ResourceAccess {
	private Collection<AuthorizationConstants.ACCESS_TYPE> accessType;

	/**
	 * @return the accessTypes
	 */
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessTypes the accessTypes to set
	 */
	public void setAccessType(Collection<AuthorizationConstants.ACCESS_TYPE> accessType) {
		this.accessType = accessType;
	}
	
}
