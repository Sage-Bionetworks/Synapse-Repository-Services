package org.sagebionetworks.repo.model;

import java.util.Collection;

public class ResourceAccess {
	private Collection<String> accessType;

	/**
	 * @return the accessTypes
	 */
	public Collection<String> getAccessType() {
		return accessType;
	}

	/**
	 * @param accessTypes the accessTypes to set
	 */
	public void setAccessType(Collection<String> accessType) {
		this.accessType = accessType;
	}
	
}
