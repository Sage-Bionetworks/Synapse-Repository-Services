package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;

/**
 * This class tracks modifications to an Node's access control list,
 * specifically Create, Update and Delete events
 * 
 */
public class AccessControlListAudit {
	private String id;
	// the node whose access control list is audited
	private String resourceId; 
	
	private Date eventDate;
	
	public enum event {CREATE, UPDATE, DELETE};
	
	// for CREATE and UPDATE events this set gives
	// the ACL for the Node after the change is applied
	private Set<ResourceAccess> resourceAccess; 

	

}
