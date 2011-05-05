package org.sagebionetworks.repo.model;

import java.util.Collection;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AuthorizationDAO {
	
	
//	/**
//	 * @param groupId
//	 * @param nodeId
//	 * @param accessTypes
//	 * @param userId the user making the request
//	 * 
//	 * sets the access that given group has for the given node
//	 * 
//	 * @exception NotFoundException if the group or node is invalid; 
//	 * UnauthorizedException if the given user doesn't have 'share'
//	 * permission on the given node
//	 * 
//	 */
//	public void setAccess(String groupId, String nodeId, Collection<String> accessTypes, String userId)
//		throws NotFoundException, UnauthorizedException, DatastoreException;
//	
//	/**
//	 * @param groupId
//	 * @param nodeId
//	 * @param userId the user making the request
//	 * 
//	 * removes the access to the given node from given group
//	 * 
//	 * @exception NotFoundException if the group or node is invalid; 
//	 * UnauthorizedException if the given user doesn't have 'share'
//	 * permission on the given node
//	 * 
//	 */
//	public void removeAccess(String groupId, String nodeId, String userId) 
//		throws NotFoundException, UnauthorizedException, DatastoreException;
	
	/**
	 * @param userName
	 * @param nodeId
	 * @param accessType
	 * 
	 * @return true iff the given group has the given access to the given node
	 * 
	 * @exception NotFoundException if the group or node is invalid
	 * 
	 */
	public boolean canAccess(String userName, String nodeId, AuthorizationConstants.ACCESS_TYPE accessType) 
		throws NotFoundException, DatastoreException;
	
	/**
	 * Provide access to the given resource for the given user.
	 * If user==null, then provide Public access to the resource.
	 * All defined access types are added: READ, CHANGE, SHARE
	 * 
	 */
	public void addUserAccess(Node node, String userName) throws NotFoundException, DatastoreException;
	
	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	public void removeAuthorization(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * @param userName
	 * @param nodeType
	 * @return true iff the user has 'create' permission for the given type
	 */
	public boolean canCreate(String userName, String nodeType) throws NotFoundException, DatastoreException;

}
