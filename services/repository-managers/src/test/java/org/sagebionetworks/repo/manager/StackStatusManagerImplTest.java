package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StackStatusManagerImplTest {
	
	@Autowired
	private StackStatusManager stackStatusManager;
	
	@Autowired
	public UserManager userManager;
	
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
	
	@Test
	public void testGetCurrent() {
		StackStatus status = stackStatusManager.getCurrentStatus();
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUpdate() throws Exception {
		// Only an admin can change the status
		StackStatus status = stackStatusManager.getCurrentStatus();
		stackStatusManager.updateStatus(testUserInfo, status);
	}
	
	@Test 
	public void testAdminUpdate() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		// Only an admin can change the status
		StackStatus status = stackStatusManager.getCurrentStatus();
		status.setPendingMaintenanceMessage("Pending the completion of this test");
		StackStatus updated = stackStatusManager.updateStatus(adminUserInfo, status);
		assertEquals(status, updated);
		// Clear the message
		status = stackStatusManager.getCurrentStatus();
		status.setPendingMaintenanceMessage(null);
		updated = stackStatusManager.updateStatus(adminUserInfo, status);
		assertEquals(status, updated);
	}
	

}
