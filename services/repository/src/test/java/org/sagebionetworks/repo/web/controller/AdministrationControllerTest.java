package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AdministrationControllerTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	public UserManager userManager;
	
	@Autowired
	public NodeManager nodeManager;
	
	@Autowired
	private StackStatusDao stackStatusDao;
	
	private List<String> toDelete;
	private Long adminUserId;
	private Project entity;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		toDelete = new ArrayList<String>();
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
	
	@After
	public void after() throws Exception {
		// Always restore the status to read-write
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);

		
		UserInfo adminUserInfo = userManager.getUserInfo(adminUserId);
		
		if(entity != null){
			try {
				nodeManager.delete(adminUserInfo, entity.getId());
			} catch (DatastoreException e) {
				// nothing to do here
			} catch (NotFoundException e) {
				// nothing to do here
			}	
		}
		
		if (nodeManager != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(adminUserInfo, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	@Test
	public void testGetNonAdminStackStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = servletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}

	@Test
	public void testGetStackStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = servletTestHelper.getAdminStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}

	@Test
	public void testUpdateStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = servletTestHelper.getAdminStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		// Make sure we can update the status
		status.setPendingMaintenanceMessage("AdministrationControllerTest.testUpdateStatus");
		StackStatus back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, status);
		assertEquals(status, back);
	}
	
	@Test
	public void testGetAndUpdateStatusWhenDown() throws Exception {
		// Make sure we can get the status when down.
		StackStatus setDown = new StackStatus();
		setDown.setStatus(StatusEnum.DOWN);
		setDown.setCurrentMessage("Synapse is going down for a test: AdministrationControllerTest.testGetStatusWhenDown");
		StackStatus back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, setDown);
		assertEquals(setDown, back);
		// Make sure we can still get the status
		StackStatus current = servletTestHelper.getAdminStackStatus(dispatchServlet);
		assertEquals(setDown, current);
		
		// Now make sure we can turn it back on when down.
		setDown.setStatus(StatusEnum.READ_WRITE);
		setDown.setCurrentMessage(null);
		back = servletTestHelper.updateStackStatus(dispatchServlet, adminUserId, setDown);
		assertEquals(setDown, back);
	}
	
	@Test
	public void testClearLocks() throws Exception{
		// Clear all locks
		servletTestHelper.clearAllLocks(dispatchServlet, adminUserId);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testClearLocksUnauthorized() throws Exception{
		// Clear all locks
		servletTestHelper.clearAllLocks(dispatchServlet, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
	}
}

