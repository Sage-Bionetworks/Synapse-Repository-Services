package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;

public interface AuthorizationDAO {
	
	
	/**
	 * Creates a user with the given name, along with the 'individual group' 
	 * needed for granting authorization
	 */
	public User createUser(String userName) throws DatastoreException;
	
	/**
	 * removes the user along with their individual group
	 */
	public void deleteUser(String userName) throws DatastoreException, NotFoundException;
	
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
