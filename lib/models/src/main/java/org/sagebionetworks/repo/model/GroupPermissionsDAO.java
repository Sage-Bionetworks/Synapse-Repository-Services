package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.web.NotFoundException;

public interface GroupPermissionsDAO extends BaseDAO<UserGroup> {
//	public void addUser(UserGroup userGroup, Long user) throws NotFoundException, DatastoreException;
//	public void removeUser(UserGroup userGroup, Long user) throws NotFoundException, DatastoreException;
//	public Collection<Long> getUsers(UserGroup userGroup) throws NotFoundException, DatastoreException;
	
	public void addResource(UserGroup userGroup, String resourceId, Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) throws NotFoundException, DatastoreException;
	public void removeResource(UserGroup userGroup, String resourceId) throws NotFoundException, DatastoreException;

	/**
	 * @param nodeId the resource whose authorization is to be removed
	 * 
	 * Removes all authorization for this resource, e.g. just before deletion.
	 */
	public void removeAuthorization(String nodeId) throws NotFoundException, DatastoreException;
	
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, String resourceId) throws NotFoundException, DatastoreException;
	
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException;

	public UserGroup createPublicGroup() throws DatastoreException;

	public UserGroup getAdminGroup() throws DatastoreException;
	
	public UserGroup createAdminGroup() throws DatastoreException;
	
	public UserGroup getIndividualGroup(String userName) throws DatastoreException;

	public UserGroup createIndividualGroup(String userName) throws DatastoreException;
	
	/**
	 * @return the NON-system groups and the admin group (if applicable) for the list of group names
	 */
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName) throws DatastoreException;

	public void setCreatableTypes(UserGroup userGroup, Collection<String> creatableTypes) throws NotFoundException, DatastoreException;

	public Collection<String> getCreatableTypes(UserGroup g) throws NotFoundException, DatastoreException;
	
	public boolean canAccess(Collection<UserGroup> groups, String resourceId, AuthorizationConstants.ACCESS_TYPE accessType) throws NotFoundException, DatastoreException;
	
	/**
	 * @return true iff some group in the list has creation privileges to the given type
	 */
	public boolean canCreate(Collection<UserGroup> groups, String creatableType)  throws NotFoundException, DatastoreException;
	
	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(AuthorizationConstants.ACCESS_TYPE accessType, List<String> groupIds);
	

}
