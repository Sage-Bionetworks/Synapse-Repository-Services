package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;

public interface UserGroupDAO extends BaseDAO<UserGroup> {
	public void addUser(UserGroup userGroup, Long user) throws NotFoundException, DatastoreException;
	public void removeUser(UserGroup userGroup, Long user) throws NotFoundException, DatastoreException;
	public Collection<Long> getUsers(UserGroup userGroup) throws NotFoundException, DatastoreException;
	
	public void addResource(UserGroup userGroup, Authorizable resource, Collection<AuthorizationConstants.ACCESS_TYPE> accessTypes) throws NotFoundException, DatastoreException;
	public void removeResource(UserGroup userGroup, Authorizable resource) throws NotFoundException, DatastoreException;
	public Collection<AuthorizationConstants.ACCESS_TYPE> getAccessTypes(UserGroup userGroup, Authorizable resource) throws NotFoundException, DatastoreException;
	
	public UserGroup getPublicGroup() throws NotFoundException, DatastoreException;

	public UserGroup createPublicGroup() throws DatastoreException;

	public UserGroup getAdminGroup() throws DatastoreException;
	
	public UserGroup createAdminGroup() throws DatastoreException;
	
	public UserGroup getIndividualGroup(String userName) throws DatastoreException;

	public UserGroup createIndividualGroup(String userName) throws DatastoreException;

	public void setCreatableTypes(UserGroup userGroup, Collection<String> creatableTypes) throws NotFoundException, DatastoreException;

	public Collection<String> getCreatableTypes(UserGroup g) throws NotFoundException, DatastoreException;
	
}
