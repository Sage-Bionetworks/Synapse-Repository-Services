package org.sagebionetworks.repo.model;


public interface UserDAO extends BaseDAO<User> {

	User getUser(String userName) throws DatastoreException;

}
