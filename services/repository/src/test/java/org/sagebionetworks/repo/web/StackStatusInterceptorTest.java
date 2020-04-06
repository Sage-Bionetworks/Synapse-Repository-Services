package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerJunit5TestBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test that the intercepter is working as expected.
 * @author John
 *
 */
public class StackStatusInterceptorTest extends AbstractAutowiredControllerJunit5TestBase {
	
	private static final String CURRENT_STATUS_2 = " for StackStatusInterceptorTest.test";
	private static final String CURRENT_STATUS_1 = "Setting the status to ";
	private static final String MSG_FORMAT = CURRENT_STATUS_1 + "%s" + CURRENT_STATUS_2;
	
	@Autowired
	private StackStatusDao stackStatusDao;
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
		
	private Project sampleProject = null;

	private Long adminUserId;
	private String accessToken;
	
	@BeforeEach
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		assertNotNull(stackStatusDao);
		sampleProject = new Project();
		// Create a sample project
		accessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);
		sampleProject = servletTestHelper.createEntity(dispatchServlet, sampleProject, accessToken);
	}
	
	@AfterEach
	public void after() throws Exception{
		// Always restore the status to READ_WRITE after each test.
		// Even it there is a failure we do not want to end up in the wrong state.
		StackStatus status = stackStatusDao.getFullCurrentStatus();
		if(StatusEnum.READ_WRITE != status.getStatus()){
			status.setStatus(StatusEnum.READ_WRITE);
			status.setCurrentMessage(null);
			status.setPendingMaintenanceMessage(null);
			stackStatusDao.updateStatus(status);
		}
		// Delete the sample project
		if(sampleProject != null && sampleProject.getId() != null){
			servletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), adminUserId);
		}
	}
	
	@Test
	public void testStatusMessage() {
		assertNull(statusMessage(StatusEnum.READ_ONLY, null));
		assertEquals("", statusMessage(StatusEnum.READ_ONLY, ""));
		assertEquals(String.format(MSG_FORMAT, StatusEnum.READ_ONLY), statusMessage(StatusEnum.READ_ONLY, MSG_FORMAT));
	}

	@Test
	public void testGetWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project fetched = servletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), accessToken);
		assertNotNull(fetched);
	}
	
	@Test
	public void testGetReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		try{
			// This should fail in read only.
			servletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), accessToken);
			fail("Calling a GET while synapse is down should have thrown an 503");
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getMessage().indexOf(StatusEnum.READ_ONLY.name()) > 0);
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}

	@Test
	public void testGetReadOnlyNullMsg() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, null);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		assertNull(stackStatusDao.getFullCurrentStatus().getCurrentMessage());
		try{
			// This should fail in read only.
			servletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), accessToken);
			fail("Calling a GET while synapse is down should have thrown an 503");
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().contains("Synapse is down for maintenance."));
		}
	}

	@Test
	public void testGetReadOnlyEmptyMsg() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, "");
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		assertEquals("", stackStatusDao.getFullCurrentStatus().getCurrentMessage());
		try{
			// This should fail in read only.
			servletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), accessToken);
			fail("Calling a GET while synapse is down should have thrown an 503");
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().contains("Synapse is down for maintenance."));
		}
	}

	@Test
	public void testGetDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		try{
			// This should fail
			servletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), accessToken);
			fail("Calling a GET while synapse is down should have thrown an 503");
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getMessage().indexOf(StatusEnum.DOWN.name()) > 0);
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}
	
	@Test
	public void testPostWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Folder child = new Folder();
		child.setParentId(sampleProject.getId());
		Folder fetched = servletTestHelper.createEntity(dispatchServlet, child, accessToken);
		assertNotNull(fetched);
	}
	
	@Test
	public void testPostReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		try{
			// This should fail in read only.
			servletTestHelper.createEntity(dispatchServlet, child, accessToken);
			fail("Calling a GET while synapse is down should have thrown an 503");
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getMessage().indexOf(StatusEnum.READ_ONLY.name()) > 0);
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}
	
	@Test
	public void testPostDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		Assertions.assertThrows(DatastoreException.class, ()-> {
			servletTestHelper.createEntity(dispatchServlet, child, accessToken);
		});
	}
	
	@Test
	public void testPutWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project fetched = servletTestHelper.updateEntity(dispatchServlet, sampleProject, accessToken);
		assertNotNull(fetched);
	}
	
	@Test
	public void testPutReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		Assertions.assertThrows(DatastoreException.class, ()-> {
			servletTestHelper.updateEntity(dispatchServlet, sampleProject, accessToken);
		});
	}
	
	@Test
	public void testPutDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Assertions.assertThrows(DatastoreException.class, ()-> {
			servletTestHelper.updateEntity(dispatchServlet, sampleProject, accessToken);
		});
	}
	
	@Test
	public void testDeleteReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		servletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), adminUserId);
		sampleProject = null;
	}

	@Test
	public void testDeleteReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		Assertions.assertThrows(DatastoreException.class, ()-> {
			servletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), adminUserId);
		});
		sampleProject = null;
	}

	@Test
	public void testDeleteDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Assertions.assertThrows(DatastoreException.class, ()-> {
			servletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), adminUserId);
		});
		sampleProject = null;
	}

	/**
	 * Helper to set the status.
	 * @param toSet
	 */
	private void setStackStatus(StatusEnum toSet, String msg){
		StackStatus status = new StackStatus();
		status.setStatus(toSet);
		status.setCurrentMessage(statusMessage(toSet, msg));
		status.setPendingMaintenanceMessage("Pending the completion of StackStatusInterceptorTest.test");
		stackStatusDao.updateStatus(status);
	}

	private String statusMessage(StatusEnum status, String msg) {
		if ((msg != null) && (! msg.isEmpty())) {
			return String.format(msg, status);
		}
		return msg;
	}

	@Test
	public void testGetVersionReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		SynapseVersionInfo versionInfo = servletTestHelper.getVersionInfo();
		assertNotNull(versionInfo);
	}

	@Test
	public void testGetVersionDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		SynapseVersionInfo versionInfo = servletTestHelper.getVersionInfo();
		assertNotNull(versionInfo);
	}

	@Test
	public void testGetStatusReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		StackStatus stackStatus = servletTestHelper.getStackStatus();
		assertNotNull(stackStatus);
	}


	@Test
	public void testGetStatusDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN, MSG_FORMAT);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		StackStatus stackStatus = servletTestHelper.getStackStatus();
		assertNotNull(stackStatus);
	}
}
