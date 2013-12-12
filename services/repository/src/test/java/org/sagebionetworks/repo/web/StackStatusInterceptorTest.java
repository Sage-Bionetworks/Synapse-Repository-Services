package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.controller.ServletTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Autowired
	private StackStatusDao stackStatusDao;
	
	@Autowired
	private UserManager userManager;
		
	private static HttpServlet dispatchServlet;
	
	private Project sampleProject = null;

	private String userName;
	
	
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
	
	@Before
	public void before() throws Exception {
		userName = userManager.getGroupName(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		assertNotNull(stackStatusDao);
		sampleProject = new Project();
		// Create a sample project
		sampleProject = ServletTestHelper.createEntity(dispatchServlet, sampleProject, 	userName);
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
		Study child = new Study();
		child.setParentId(sampleProject.getId());
		Study fetched = ServletTestHelper.createEntity(dispatchServlet, child, userName);
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
		} catch (DatastoreException e){
			// Make sure the message is in the exception
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_1) > 0);
			assertTrue(e.getMessage().indexOf(StatusEnum.READ_ONLY.name()) > 0);
			assertTrue(e.getMessage().indexOf(CURRENT_STATUS_2) > 0);
		}
	}
	
	@Test (expected=DatastoreException.class)
	public void testPostDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		Project child = new Project();
		child.setParentId(sampleProject.getId());
		ServletTestHelper.createEntity(dispatchServlet, child, userName);
		fail();
	}
	
	@Test
	public void testPutWithReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		Project fetched = ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		assertNotNull(fetched);
	}
	
	@Test (expected=DatastoreException.class)
	public void testPutReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		fail();
	}
	
	@Test (expected=DatastoreException.class)
	public void testPutDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.updateEntity(dispatchServlet, sampleProject, userName);
		fail();
	}
	
	@Test
	public void testDeleteReadWrite() throws Exception {
		// We should be able to get when the status is read-write
		assertEquals(StatusEnum.READ_WRITE, stackStatusDao.getCurrentStatus());
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
	}
	
	@Test (expected=DatastoreException.class)
	public void testDeleteReadOnly() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.READ_ONLY);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.READ_ONLY, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
		fail();
	}
	
	@Test (expected=DatastoreException.class)
	public void testDeleteDown() throws Exception {
		// Set the status to be read only
		setStackStatus(StatusEnum.DOWN);
		// Make sure the status is what we expect
		assertEquals(StatusEnum.DOWN, stackStatusDao.getCurrentStatus());
		// This should fail
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, sampleProject.getId(), userName);
		sampleProject = null;
		fail();
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

}
