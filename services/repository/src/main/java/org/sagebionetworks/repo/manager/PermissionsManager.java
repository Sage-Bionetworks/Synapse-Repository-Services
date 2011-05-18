package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface PermissionsManager {
	/**
	 * Invoked for a node which currently inherits its permissions, this
	 * method overrides inheritance so that the node defines its own
	 * permissions, and then assigns permissions based on the passed parameter.
	 * 
	 * @exception if the user invoking this method doesn't have the required authority
	 * 
	 * TODO:  should we throw an exception if the node already doesn't inherit, or is it a valid 'update' operation?
	 */
	public AccessControlList overrideInheritance(AccessControlList acl, String userName);
	
	/**
	 * Gets the access control list (ACL) governing the given node id.
	 * If the given node inherits access control from a parent or
	 * ancestor, this method returns the ACL of the governing node.
	 * 
	 * I.e. caller can determine whether a node inherits its
	 * permissions by comparing the nodeId parameter to the 
	 * 'resourceId' field in the returned object
	 */
	public AccessControlList getACL(String nodeId, String userName) throws NotFoundException;
	
	/**
	 * Update the given ACL, as keyed by the 'resourceId' field
	 * in the given acl.
	 * 
	 * @exception if the node in the given ACL inherits its permissions
	 * 
	 * @exception if the user invoking this method doesn't have the required authority
	 */
	public AccessControlList updateACL(AccessControlList acl, String userName);
	
	/**
	 * Invoked for a node which assigns its own permissions.  Makes
	 * the node inherit its permissions from the same node from which
	 *  its parent inherits.  Returns the ACL for the node from which
	 *  the given node now inherits.
	 * 
	 * @exception if the user invoking this method doesn't have the required authority
	 *  
	 *  TODO:  should we throw an exception if the node already inherits, or is it a valid no-op?
	 */
	public AccessControlList restoreInheritance(String nodeId, String userName);

}
