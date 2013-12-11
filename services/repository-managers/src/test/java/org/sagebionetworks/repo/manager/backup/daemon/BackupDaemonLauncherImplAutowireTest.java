package org.sagebionetworks.repo.manager.backup.daemon;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BackupDaemonLauncherImplAutowireTest {

	@Autowired
	private BackupDaemonLauncher backupDaemonLauncher;
	
	@Autowired
	private UserManager userManager;
	
	private UserInfo testUserInfo;
	
	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@");
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
	}
	
	@After
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(testUserInfo.getIndividualGroup().getId()));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUserGetStatus() throws UnauthorizedException, DatastoreException, NotFoundException{
		// A non-admin should not be able to start the daemon
		backupDaemonLauncher.getStatus(testUserInfo, "123");
	}
}
