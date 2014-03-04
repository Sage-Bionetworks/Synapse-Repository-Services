package org.sagebionetworks.repo.manager.doi;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
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
	
	private Long adminUserId;
	private Long testUserId;
	private UserInfo adminUserInfo;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
	}
	
	@After
	public void after() throws Exception {
		userManager.deletePrincipal(adminUserInfo, testUserId);
	}

	@Test
	public void testAdmin() throws Exception {
		doiAdminManager.clear(adminUserId);
	}

	@Test(expected=UnauthorizedException.class)
	public void testNotAdmin() throws Exception {
		doiAdminManager.clear(testUserId);
	}
}
