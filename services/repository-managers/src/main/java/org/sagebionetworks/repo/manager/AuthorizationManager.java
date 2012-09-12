package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AuthorizationManager {
	
	/**
	 * @param userInfo
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given user has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(UserInfo userInfo, String nodeId, ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException;
		
	/**
    *
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff either (1) the user has 'add child' access to the parent or (2) parent is null
	 * and user is admin
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canCreate(UserInfo userInfo, final Node node) throws NotFoundException, DatastoreException ;
	
	/**
	 * @param n the number of items in the group-id list
	 * 
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(int n);

	/**
	 * Get the user's permissions for an entity.
	 * @param userInfo
	 * @param entityId
	 * @return 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException;

}
