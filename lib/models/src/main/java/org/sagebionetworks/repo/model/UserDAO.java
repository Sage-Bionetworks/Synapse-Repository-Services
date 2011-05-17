package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;


public interface UserDAO extends BaseDAO<User> {

	User getUser(String userName) throws DatastoreException, NotFoundException;

}
