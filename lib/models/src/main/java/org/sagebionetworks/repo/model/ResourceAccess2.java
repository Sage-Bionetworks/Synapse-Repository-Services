package org.sagebionetworks.repo.model;

import java.util.Set;

public class ResourceAccess2 {
	private String userGroupId;
	
	/**
	 * valid access types include:
	 * READ
	 * CHANGE
	 * CHANGE_PERMISSIONS
	 * ADD_CHILD
	 * REMOVE_CHILD
	 * 
	 */
	
	private Set<String> accessType;
}
