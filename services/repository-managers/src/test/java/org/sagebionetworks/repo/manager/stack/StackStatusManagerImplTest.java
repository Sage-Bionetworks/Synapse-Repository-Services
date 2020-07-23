package org.sagebionetworks.repo.manager.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StackStatusManagerImplTest {
	
	@Autowired
	private StackStatusManager stackStatusManager;
	
	@Autowired
	public UserManager userManager;
	
	private UserInfo testUserInfo;
	
	@BeforeEach
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
	}
	
	@AfterEach
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		userManager.deletePrincipal(adminUserInfo, testUserInfo.getId());
	}
	
	@Test
	public void testGetCurrent() {
		StackStatus status = stackStatusManager.getCurrentStatus();
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test
	public void testNonAdminUpdate() throws Exception {
		// Only an admin can change the status
		StackStatus status = stackStatusManager.getCurrentStatus();
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			stackStatusManager.updateStatus(testUserInfo, status);
		}).getMessage();
		
		assertEquals("Must be an administrator to change the status of the stack", errorMessage);
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
