package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
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
	public boolean canAccess(UserInfo userInfo, String nodeId, AuthorizationConstants.ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException;
	
//	/**
//	 * Provide access to the given resource for the given user.
//	 * All defined access types are added: READ, CHANGE, SHARE
//	 * 
//	 */
//	public void addUserAccess(Node node, UserInfo userInfo) throws NotFoundException, DatastoreException;
	
	/**
	 * @param userInfo
	 * @param nodeType
	 * @return true iff the user has 'create' permission for the given type
	 */
	public boolean canCreate(UserInfo userInfo, String nodeType) throws NotFoundException, DatastoreException;
	
// this is only used internally to the manager, no need to expose publicly
//	/**
//	 * @param userGroups a collection of groups
//	 * @return true iff one of the groups is the admin group
//	 */ 
//	public boolean isAdmin(UserInfo userInfo) throws DatastoreException, NotFoundException;
		
	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	public void removeAuthorization(String nodeId) throws NotFoundException, DatastoreException;

	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(AuthorizationConstants.ACCESS_TYPE accessType, List<String> groupIds);

}
