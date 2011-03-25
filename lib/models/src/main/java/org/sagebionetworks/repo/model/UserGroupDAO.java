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
	
	public void addResource(UserGroup userGroup, Base resource, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
	public void removeResource(UserGroup userGroup, Base resource, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	// I'm not sure that these are really needed, I think it's more likely that permissions will affect 
	// filtering during other queries, rather than a client needing to know everything a group can access
//	public Collection<Base> getResources(UserGroup userGroup) throws NotFoundException, DatastoreException, UnauthorizedException;
//	public Collection<Base> getResources(UserGroup userGroup, String accessType) throws NotFoundException, DatastoreException, UnauthorizedException;
//	public Collection<String> getAccessTypes(UserGroup userGroup, Base resource) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException, UnauthorizedException;
	
}
