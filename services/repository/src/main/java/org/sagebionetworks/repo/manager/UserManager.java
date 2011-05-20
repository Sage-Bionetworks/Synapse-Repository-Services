package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface UserManager {

	/**
	 * Get the User and UserGroup information for the given user name.
	 * Has the side effect of creating permissions-related objects for the
	 * groups that the user is in.
	 * 
	 */
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException;
	
	// for testing
	public void setUserDAO(UserDAO userDAO);

	

}
