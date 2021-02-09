package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;

public interface EntityAuthorizationManager {

	/**
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param accessType
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public AuthorizationStatus hasAccess(UserInfo userInfo, String entityId, ACCESS_TYPE accessType)
			throws NotFoundException, DatastoreException;

	/**
	 * 
	 * @param userInfo
	 * @param entityIds
	 * @param accessType
	 * @return
	 */
	public List<UsersEntityAccessInfo> batchHasAccess(UserInfo userInfo, List<Long> entityIds, ACCESS_TYPE accessType);

	/**
	 * 
	 * @param userInfo
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException;

}
