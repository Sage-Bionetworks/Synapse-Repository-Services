package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AdministrationControllerTest {

	@Autowired
	public UserManager userManager;
	
	@Autowired
	public NodeManager nodeManager;
	
	private static HttpServlet dispatchServlet;
	
	@Autowired
	private StackStatusDao stackStatusDao;
	
	private List<String> toDelete;
	private UserInfo adminUserInfo;
	private Project entity;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		toDelete = new ArrayList<String>();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@BeforeClass
	public static void beforeClass() throws ServletException {
		dispatchServlet = DispatchServletSingleton.getInstance();
	}

	@After
	public void after() throws UnauthorizedException {
		// Always restore the status to read-write
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);

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
	public void testGetStackStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = ServletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
	}
	
	@Test
	public void testUpdateStatus() throws Exception {
		// Make sure we can get the stack status
		StackStatus status = ServletTestHelper.getStackStatus(dispatchServlet);
		assertNotNull(status);
		assertEquals(StatusEnum.READ_WRITE, status.getStatus());
		// Make sure we can update the status
		status.setPendingMaintenanceMessage("AdministrationControllerTest.testUpdateStatus");
		StackStatus back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), status);
		assertEquals(status, back);
	}
	
	@Test
	public void testGetAndUpdateStatusWhenDown() throws Exception {
		// Make sure we can get the status when down.
		StackStatus setDown = new StackStatus();
		setDown.setStatus(StatusEnum.DOWN);
		setDown.setCurrentMessage("Synapse is going down for a test: AdministrationControllerTest.testGetStatusWhenDown");
		StackStatus back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), setDown);
		assertEquals(setDown, back);
		// Make sure we can still get the status
		StackStatus current = ServletTestHelper.getStackStatus(dispatchServlet);
		assertEquals(setDown, current);
		
		// Now make sure we can turn it back on when down.
		setDown.setStatus(StatusEnum.READ_WRITE);
		setDown.setCurrentMessage(null);
		back = ServletTestHelper.updateStackStatus(dispatchServlet, adminUserInfo.getIndividualGroup().getName(), setDown);
		assertEquals(setDown, back);
	}
}
