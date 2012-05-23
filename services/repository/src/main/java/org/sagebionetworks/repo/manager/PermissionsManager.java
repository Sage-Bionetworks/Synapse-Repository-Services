package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
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
	 * @param nodeId - The id of the node to get the ACL for.
	 * @param userInfo
	 * @return
	 * @throws NotFoundException - Thrown if the nodeID does not exist.
	 * @throws DatastoreException - Server-side error.
	 * @throws ACLInheritanceException - Thrown when attempting to get the ACL for a node that inherits its permissions. The exception
	 * will include the benefactor's ID. 
	 */
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException;
	
	/**
	 * Get the permissions benefactor of an entity.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String getPermissionBenefactor(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException;
	
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
	public Collection<UserGroup> getGroups(UserInfo userInfo) throws DatastoreException, UnauthorizedException;
	
	/**
	 * get all individual user groups
	 * 
	 **/
	public Collection<UserGroup> getIndividuals(UserInfo userInfo) throws DatastoreException, UnauthorizedException;
	
	/**
	 * get non-individual user groups (including Public) in range
	 * 
	 **/
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
	
	/**
	 * get individual user groups in range
	 * 
	 **/
	public List<UserGroup> getIndividualsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException;
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resourceId the ID of the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(String resourceId, ACCESS_TYPE  accessType, UserInfo userInfo) throws NotFoundException, DatastoreException;

	/**
	 * Get the user permission for an entity.
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException;
}
