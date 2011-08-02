package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface PermissionsManager {
	/**
	 * Invoked for a node which currently inherits its permissions, this
	 * method overrides inheritance so that the node defines its own
	 * permissions, and then assigns permissions based on the passed parameter.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws ConflictingUpdateException 
	 * 
	 * @exception if the user invoking this method doesn't have the required authority
	 * 
	 */
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Gets the access control list (ACL) governing the given node id.
	 * If the given node inherits access control from a parent or
	 * ancestor, this method returns the ACL of the governing node.
	 * 
	 * I.e. caller can determine whether a node inherits its
	 * permissions by comparing the nodeId parameter to the 
	 * 'resourceId' field in the returned object
	 */
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException;
	
	/**
	 * Update the given ACL, as keyed by the 'resourceId' field
	 * in the given acl.
	 * @throws UnauthorizedException 
	 * 
	 * @exception if the node in the given ACL inherits its permissions
	 * 
	 * @exception if the user invoking this method doesn't have the required authority
	 */
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo)  throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Invoked for a node which assigns its own permissions.  Makes
	 * the node inherit its permissions from the same node from which
	 *  its parent inherits.  Returns the ACL for the node from which
	 *  the given node now inherits.
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws ConflictingUpdateException 
	 * 
	 * @exception if the user invoking this method doesn't have the required authority or if
	 * the resource already inherits permissions
	 *  
	 */
	public AccessControlList restoreInheritance(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	// the following methods provide the principals (individuals and groups) 
	// to which resource access permission may be granted
	
	/**
	 * 
	 * get all non-individual user groups, including Public
	 * @throws DatastoreException 
	 * 
	 **/
	public Collection<UserGroup> getGroups() throws DatastoreException;
	
	/**
	 * get all individual user groups
	 * 
	 **/
	public Collection<UserGroup> getIndividuals() throws DatastoreException;
	
	/**
	 * get non-individual user groups (including Public) in range
	 * 
	 **/
	public List<UserGroup> getGroupsInRange(long startIncl, long endExcl) throws DatastoreException;
	
	/**
	 * get individual user groups in range
	 * 
	 **/
	public List<UserGroup> getIndividualsInRange(long startIncl, long endExcl) throws DatastoreException;
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resourceId the ID of the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(String resourceId, AuthorizationConstants.ACCESS_TYPE  accessType, UserInfo userInfo) throws NotFoundException, DatastoreException ;
}
