package org.sagebionetworks.repo.web.util;

import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides an admin user for testing.
 * 
 * @author jmhill
 *
 */
public class UserProviderImpl implements UserProvider, InitializingBean {
	
	@Autowired
	public UserManager userManager;
	
	private UserInfo testAdminUser = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Create the test admin user
		testAdminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
	}
	
	/**
	 * This is an admin user that can be used for testing.
	 * @return
	 */
	public UserInfo getTestAdiminUserInfo(){
		return testAdminUser;
	}
	

}
