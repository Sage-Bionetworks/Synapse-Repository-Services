package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StackStatusManagerImplTest {
	
	@Autowired
	StackStatusManager stackStatusManager;
	@Autowired
	public UserProvider testUserProvider;
	@Test
	public void testGetCurrent(){
		StackStatus status = stackStatusManager.getCurrentStatus();
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testNonAdminUpdate() throws UnauthorizedException{
		// Only an admin can change the status
		StackStatus status = stackStatusManager.getCurrentStatus();
		stackStatusManager.updateStatus(testUserProvider.getTestUserInfo(), status);
	}
	
	@Test 
	public void testAdminUpdate() throws UnauthorizedException{
		// Only an admin can change the status
		StackStatus status = stackStatusManager.getCurrentStatus();
		status.setPendingMaintenanceMessage("Pending the completion of this test");
		StackStatus updated = stackStatusManager.updateStatus(testUserProvider.getTestAdminUserInfo(), status);
		assertEquals(status, updated);
		// Clear the message
		status = stackStatusManager.getCurrentStatus();
		status.setPendingMaintenanceMessage(null);
		updated = stackStatusManager.updateStatus(testUserProvider.getTestAdminUserInfo(), status);
		assertEquals(status, updated);
	}
	

}
