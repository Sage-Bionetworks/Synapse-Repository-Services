package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.BackupSubmission;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.controller.ServletTestHelper;
import org.sagebionetworks.repo.web.controller.ServletTestHelperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Test that the intercepter is working as expected.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StackStatusInterceptorTest {
	
	private static final String CURRENT_STATUS_2 = " for StackStatusInterceptorTest.test";

	private static final String CURRENT_STATUS_1 = "Setting the status to ";

	private static final long TIMEOUT = 1000*60*1; // 1 minutes
	
	@Autowired
	StackStatusDao stackStatusDao;
		
	private static HttpServlet dispatchServlet;
	
	private Project sampleProject = null;

	private String userName = TestUserDAO.ADMIN_USER_NAME;
	
	
	@BeforeClass
	public static void beforeClass() throws ServletException {
		// Setup the servlet once
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status
		// code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",	"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);
	}
	
	
	@After
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
		if(sampleProject != null){
			ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		}
	}
	
	@Before
	public void before() throws Exception {
		assertNotNull(stackStatusDao);
		sampleProject = new Project();
		// Create a sample project
		sampleProject = ServletTestHelper.createEntity(dispatchServlet, sampleProject, 	userName);
	}
	
	@Test
	public void testGetWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project fetched = ServletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		assertNotNull(fetched);
	}
	
	@Test
	public void testGetReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		Project fetched = ServletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		assertNotNull(fetched);
	}
	
	@Test
	public void testGetDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		try{
			// This should fail
			ServletTestHelper.getEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
			fail("Calling a GET while synapse is down should have thrown an 503");
		}catch(ServletTestHelperException e){
			assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getHttpStatus());
			// Make sure the message is in the exception
			assertTrue(e.getServiceErrorMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getServiceErrorMessage().indexOf(StatusEnum.DOWN.name()) > 0);
			assertTrue(e.getServiceErrorMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}
	
	@Test
	public void testPostWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		Project fetched = ServletTestHelper.createEntity(dispatchServlet, child, userName);
		assertNotNull(fetched);
	}
	
	@Test
	public void testPostReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		try{
			// This should fail in read only.
			ServletTestHelper.createEntity(dispatchServlet, child, userName);
			fail("Calling a GET while synapse is down should have thrown an 503");
		}catch(ServletTestHelperException e){
			assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getHttpStatus());
			// Make sure the message is in the exception
			assertTrue(e.getServiceErrorMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getServiceErrorMessage().indexOf(StatusEnum.READ_ONLY.name()) > 0);
			assertTrue(e.getServiceErrorMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}
	
	@Test (expected=ServletTestHelperException.class)
	public void testPostDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		Project fetched = ServletTestHelper.createEntity(dispatchServlet, child, userName);
		assertNotNull(fetched);
	}
	
	@Test
	public void testPutWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project fetched = ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		assertNotNull(fetched);
	}
	
	@Test (expected=ServletTestHelperException.class)
	public void testPutReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		Project fetched = ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		assertNotNull(fetched);
	}
	
	@Test (expected=ServletTestHelperException.class)
	public void testPutDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Project fetched = ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		assertNotNull(fetched);
	}
	
	@Test
	public void testDeleteReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
	}
	
	@Test (expected=ServletTestHelperException.class)
	public void testDeleteReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
	}
	
	@Test (expected=ServletTestHelperException.class)
	public void testDeleteDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
	}
	
	@Test
	public void testBackupAndRestoreWhileDown() throws Exception {
		// Make sure we can create a back while down
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		doBackupRestoreRoundTrip();
	}


	/**
	 * Helper method do do a backup and restore of samle project.
	 * @throws ServletException
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws UnauthorizedException
	 */
	public void doBackupRestoreRoundTrip() throws ServletException,
			IOException, DatastoreException, NotFoundException,
			InterruptedException, UnauthorizedException {
		BackupSubmission submission = new BackupSubmission();
		submission.setEntityIdsToBackup(new HashSet<String>());
		submission.getEntityIdsToBackup().add(sampleProject.getId());
		BackupRestoreStatus status = ServletTestHelper.startBackup(dispatchServlet, userName, submission);
		// Wait for the backup to complete
		status = waitForStatus(DaemonStatus.COMPLETED, status.getId());
		// Now make sure we can restore.
		assertNotNull(status.getBackupUrl());
		String fullUrl = status.getBackupUrl();
		int index = fullUrl.lastIndexOf("/");
		String fileName = status.getBackupUrl().substring(index+1, fullUrl.length());
		// Now restore the nodes from the backup
		RestoreSubmission file = new RestoreSubmission();
		file.setFileName(fileName);
		status = ServletTestHelper.startRestore(dispatchServlet, userName, file);
		// Wait for the backup to complete
		status = waitForStatus(DaemonStatus.COMPLETED, status.getId());
	}
	
	/**
	 * Helper to set the status.
	 * @param toSet
	 */
	private void setStackStatus(StatusEnum toSet){
		StackStatus status = new StackStatus();
		status.setStatus(toSet);
		status.setCurrentMessage(CURRENT_STATUS_1+toSet+CURRENT_STATUS_2);
		status.setPendingMaintenanceMessage("Pending the completion of StackStatusInterceptorTest.test");
		stackStatusDao.updateStatus(status);
	}
	
	/**
	 * Helper method to wait for a given status of the Daemon
	 * @param lookinFor
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private BackupRestoreStatus waitForStatus(DaemonStatus lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException, UnauthorizedException, ServletException, IOException{
		BackupRestoreStatus status = ServletTestHelper.getDaemonStatus(dispatchServlet, userName, id);
		long start = System.currentTimeMillis();
		long elapse = 0;
		while(!lookinFor.equals(status.getStatus())){
			// Wait for it to complete
			Thread.sleep(1000);
			long end =  System.currentTimeMillis();
			elapse = end-start;
			if(elapse > TIMEOUT){
				fail("Timmed out waiting for the backup deamon to finish");
			}
			status = ServletTestHelper.getDaemonStatus(dispatchServlet, userName, id);
			assertEquals(id, status.getId());
			System.out.println(DaemonStatusUtil.printStatus(status));
			if(DaemonStatus.FAILED != lookinFor && DaemonStatus.FAILED.equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}
	

}
