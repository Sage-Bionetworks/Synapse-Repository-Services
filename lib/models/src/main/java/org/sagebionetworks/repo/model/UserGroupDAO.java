package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Note: The access types can transcend those used by the datastore:  The datastore
 * might have access types 'read', 'change', and 'share' for the meta-data, but a clien
 * could use the access type 'download' to control who can access the data files referenced
 * by the meta-data.
 * 
 * @author bhoff
 *
 */
public interface UserGroupDAO extends BaseDAO<UserGroup> {
	public void addUser(UserGroup userGroup, User user) throws NotFoundException, DatastoreException, UnauthorizedException;
	public void removeUser(UserGroup userGroup, User user) throws NotFoundException, DatastoreException, UnauthorizedException;
	public Collection<User> getUsers(UserGroup userGroup) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public void addResource(UserGroup userGroup, Base resource, Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) throws NotFoundException, DatastoreException, UnauthorizedException;
	public void removeResource(UserGroup userGroup, Base resource) throws NotFoundException, DatastoreException, UnauthorizedException;
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, Base resource) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException, UnauthorizedException;
	
}
