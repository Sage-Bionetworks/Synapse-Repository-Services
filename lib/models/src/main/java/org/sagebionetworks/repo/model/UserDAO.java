package org.sagebionetworks.repo.model;

import java.util.Collection;

import org.sagebionetworks.repo.web.NotFoundException;


public interface UserDAO extends BaseDAO<User> {

	User getUser(String userName) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the groups that a user belongs to, INCLUDING the admin group, if a member,
	 * but excluding individual groups and the Public group (since all are members implicitly).
	 */
	Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException;
}
