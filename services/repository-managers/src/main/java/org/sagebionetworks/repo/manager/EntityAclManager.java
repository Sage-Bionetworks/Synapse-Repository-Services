package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for 
 * @author John
 *
 */
public interface EntityAclManager {
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

	/**
	 * Check whether or not a given resource/entity has a local ACL. Such
	 * resources/entities are self-benefactors.
	 *
	 * @param resourceId
	 * @return
	 */
	public boolean hasLocalACL(String resourceId);

	/**
	 * Get the set of children IDs that the caller does not have read access for a given parentId.
	 * @param user
	 * @param parentId
	 * @return
	 */
	public Set<Long> getNonvisibleChildren(UserInfo user, String parentId);
	
}
