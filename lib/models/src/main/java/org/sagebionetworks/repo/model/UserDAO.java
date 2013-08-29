package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;


public interface UserDAO {
	/**
	 * Fetches info on the user based off the username
	 */
	User getUser(String userName) throws DatastoreException, NotFoundException;
}
