package org.sagebionetworks.repo.manager.doi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DoiAdminManagerImplAutowiredTest {

	@Autowired 
	private DoiAdminManager doiAdminManager;
	
	@Autowired
	private UserManager userManager;
	private UserInfo testAdminUserInfo;
	private UserInfo testUserInfo;

	@Before
	public void before() throws Exception {
		testAdminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
		testUserInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
	}

	@Test
	public void testAdmin() throws Exception {
		doiAdminManager.clear(testAdminUserInfo.getIndividualGroup().getName());
	}

	@Test(expected=UnauthorizedException.class)
	public void testNotAdmin() throws Exception {
		doiAdminManager.clear(testUserInfo.getIndividualGroup().getName());
	}
}
