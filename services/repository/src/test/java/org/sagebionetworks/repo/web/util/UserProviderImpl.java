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
	private UserInfo testUser = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Create the test admin user
		testAdminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		testUser = userManager.getUserInfo(TestUserDAO.TEST_USER_NAME);
	}
	
	@Override
	public UserInfo getTestAdminUserInfo(){
		return testAdminUser;
	}

	@Override
	public UserInfo getTestUserInfo() {
		return testUser;
	}
	

}
