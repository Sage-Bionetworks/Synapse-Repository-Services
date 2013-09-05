package org.sagebionetworks.repo.web.util;

import java.util.Collection;

import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
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
	
	private UserGroup identifedUsers = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Create the test admin user
		UserInfo testAdminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		if (!testAdminUser.isAdmin()) throw new IllegalStateException(TestUserDAO.ADMIN_USER_NAME+" should be an administrator.");
		UserInfo testUser = userManager.getUserInfo(TestUserDAO.TEST_USER_NAME);
		Collection<UserGroup> groups = testUser.getGroups();
		for(UserGroup group : groups) {
			if(AuthorizationConstants.PUBLIC_GROUP_NAME.equals(group.getName())){
				identifedUsers = group;
				break;
			}
		}
		if(identifedUsers == null) throw new IllegalStateException("Cannot find the Public group");
	}
	
	@Override
	public UserInfo getTestAdminUserInfo(){
		try {
			return userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserInfo getTestUserInfo() {
		try {
			return userManager.getUserInfo(TestUserDAO.TEST_USER_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public UserGroup getIdentifiedUserGroup(){
		return identifedUsers;
	}
	

}
