package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityAuthorizationManager {

	/**
	 * Determine if the user is authorized to access the given entity. Each access
	 * type will be tested in the order provided. The first 'access-denied'
	 * encountered will be returned. An 'authorized' status will only be returned if
	 * the user has access to each of the provided types.
	 * 
	 * This method should not throw any exceptions, instead, if there is an error
	 * the resulting AuthorizationStatus will contain the error message/exception.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param accessType The permission/permissions check to be checked against the
	 *                   entity.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE...accessType);

	/**
	 * Determine if the user is authorized access the given batch of entityIds. This
	 * method should not throw any exceptions, instead, if there is an error with
	 * any individual entity, the results for that entity will include the error.
	 * 
	 * @param userInfo
	 * @param entityIds  The batch of entity IDs.
	 * @param accessType The permission check to be checked against each entity in
	 *                   the batch.
	 * @return
	 */
	List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType);

	/**
	 * Get a bundle of all of the permission that the user has on a single entity.
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException;
	

	/**
	 * Get a bundle of all the permission that the user has on a single entity.
	 * @param userInfo
	 * @param entityId
	 * @param stateProvider
	 * @return
	 */
	UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId, EntityStateProvider stateProvider);
	
	/**
	 * Can the user create an entity within the given parent and of the given entity type?
	 * @param parentId
	 * @param entityCreateType
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AuthorizationStatus canCreate(String parentId, EntityType entityCreateType, UserInfo userInfo) throws DatastoreException, NotFoundException;

	/**
	 * Can the user delete the ACL on the given entity?
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	AuthorizationStatus canDeleteACL(UserInfo userInfo, String entityId);

	/**
	 * Can the user create a wiki for the given entity?  The user must have the CREATE permission
	 * on the entity.  In addition, the user must be certified to create a wiki on any entity that
	 * is not a project.
	 * @param entityId
	 * @param userInfo
	 * @return
	 */
	AuthorizationStatus canCreateWiki(String entityId, UserInfo userInfo);

	/**
	 * For the given batch of entity ids create a list of actions that
	 * the user will need to take in order to download any file that they are
	 * currently not authorized to download.
	 * 
	 * @param userInfo
	 * @param entityIds
	 * @return
	 */
	List<FileActionRequired> getActionsRequiredForDownload(UserInfo userInfo, List<Long> entityIds);
	
}
