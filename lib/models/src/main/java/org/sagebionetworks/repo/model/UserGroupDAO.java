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
	
	public void addResource(UserGroup userGroup, String resourceId, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
	public void removeResource(UserGroup userGroup, String resourceId, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
	public Collection<String> getResources(UserGroup userGroup) throws NotFoundException, DatastoreException, UnauthorizedException;
	public Collection<String> getResources(UserGroup userGroup, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
	public Collection<String> getAccessTypes(UserGroup userGroup, String resourceId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException, UnauthorizedException;
}
