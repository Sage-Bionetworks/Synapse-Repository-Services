package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
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
	
	/**
	 * Get a default group.
	 * @param group
	 * @return
	 * @throws DatastoreException
	 */
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group) throws DatastoreException;
	
	/**
	 * Delete a user.
	 * @param id
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void deleteUser(String id) throws DatastoreException, NotFoundException;
	

	/**
	 * Find a group.
	 * @param name
	 * @param b
	 * @return
	 * @throws DatastoreException 
	 */
	public UserGroup findGroup(String name, boolean b) throws DatastoreException;

	

}
