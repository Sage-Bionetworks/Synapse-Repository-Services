package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;


public interface UserDAO {
	/**
	 * Fetches info on the user based off the username
	 */
	public User getUser(String userName) throws DatastoreException, NotFoundException;
	
	/**
	 * Throws an UnsupportedOperationException
	 * OR allows a test DAO to cleanup
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;
}
