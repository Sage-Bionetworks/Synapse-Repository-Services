package org.sagebionetworks.repo.web.util;

import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;

/**
 * Provides users for tests.
 * 
 * @author jmhill
 *
 */
public interface UserProvider {
	
	/**
	 * This is an administrator user that can be used for testing.
	 * @return userInfo for the admin user
	 */
	public UserInfo getTestAdminUserInfo();
	
	/**
	 * This is a regular user that can be used for testing.
	 * @return userInfo for the regular user
	 */
	public UserInfo getTestUserInfo();
	
	/**
	 * This is the group that all identified users belong to.
	 * @return
	 */
	public UserGroup getIdentifiedUserGroup();

}
