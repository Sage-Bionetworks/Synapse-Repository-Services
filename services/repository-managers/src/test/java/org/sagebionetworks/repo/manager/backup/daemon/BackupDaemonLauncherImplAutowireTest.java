package org.sagebionetworks.repo.manager.backup.daemon;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	
	@Autowired
	public NodeManager nodeManager;
	
	private List<String> nodesToDelete = null;
	private UserInfo adminUserInfo;
	
	@Before
	public void before() throws Exception {
		nodesToDelete = new ArrayList<String>();
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
	}
	
	@After
	public void after() throws Exception {
		for (String id: nodesToDelete) {
			nodeManager.delete(adminUserInfo, id);
		}
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUserGetStatus() throws UnauthorizedException, DatastoreException, NotFoundException{
		// A non-admin should not be able to start the daemon
		UserInfo nonAdmin = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		backupDaemonLauncher.getStatus(nonAdmin, "123");
	}
}
