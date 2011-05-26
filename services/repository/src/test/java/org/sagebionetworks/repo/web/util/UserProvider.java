package org.sagebionetworks.repo.web.util;

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
	 * @return
	 */
	public UserInfo getTestAdiminUserInfo();

}
