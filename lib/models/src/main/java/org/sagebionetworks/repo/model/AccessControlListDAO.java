package org.sagebionetworks.repo.model;

import java.util.Collection;

public interface AccessControlListDAO extends BaseDAO<AccessControlList> {
	
	/**
	 * Find the access control list for the given resource
	 * @throws DatastoreException 
	 */
	public AccessControlList getForResource(String rid) throws DatastoreException;

	/**
	 * @return true iff some group in 'groups' has explicit permission to access 'resourceId' using access type 'accessType'
	 * @throws DatastoreException 
	 */
	public boolean canAccess(Collection<UserGroup> groups, String resourceId, AuthorizationConstants.ACCESS_TYPE accessType) throws DatastoreException;

	/**
	 * @return the SQL to find the root-accessible nodes that a specified user-group list can access
	 * using a specified access type
	 */
	public String authorizationSQL(int n);

	/**
	 * executes the authorization query returned by authorizationSQL()
	 */
	public Collection<Object> execAuthorizationSQL(Collection<Long> groupIds, AuthorizationConstants.ACCESS_TYPE type);

}
