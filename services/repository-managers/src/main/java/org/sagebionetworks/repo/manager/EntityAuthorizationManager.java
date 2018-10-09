package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityAuthorizationManager {
	/**
	 * Use case:  Need to find out if a user can access a resource.
	 * 
	 * @param resourceId the ID of the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public AuthorizationStatus hasAccess(String resourceId, ACCESS_TYPE  accessType, UserInfo userInfo) throws NotFoundException, DatastoreException;

	/**
	 * Get the user permission for an entity.
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException;

	/**
	 * 
	 * @param entityId
	 * @param userInfo
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AuthorizationStatus canCreate(String parentId, EntityType nodeType, UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param entityId
	 * @param userInfo
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public AuthorizationStatus canChangeSettings(Node node, UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param user
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * Get the set of children IDs that the caller does not have read access for a given parentId.
	 * @param user
	 * @param parentId
	 * @return
	 */
	public Set<Long> getNonvisibleChildren(UserInfo user, String parentId);
}
