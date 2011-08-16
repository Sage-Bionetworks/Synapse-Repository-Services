package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * This is a an integration test for the DefaultController.
 * 
 * @author jmhill
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@Deprecated // all new tests should be added to DefaultControllerAutowiredAllTypesTest where every type is tested for every method.
public class DefaultControllerAutowiredTest {

	// Used for cleanup
	@Autowired
	GenericEntityController entityController;
	
	@Autowired
	public UserManager userManager;

	static private Log log = LogFactory
			.getLog(DefaultControllerAutowiredTest.class);

	private static HttpServlet dispatchServlet;
	
	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
	}

	@After
	public void after() throws UnauthorizedException {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(userName, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	@BeforeClass
	public static void beforeClass() throws ServletException {
		// Setup the servlet once
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status
		// code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig.addInitParameter("contextConfigLocation",
				"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);

	}

	@Test(expected = NotFoundException.class)
	public void testDelete() throws Exception {
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		ServletTestHelper.deleteEntity(dispatchServlet, Project.class, clone.getId(), userName);
		// This should throw an exception
		HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
		entityController.getEntity(userName,	clone.getId(), mockRequest, Project.class);
	}
	
	@Test
	public void testGetSchema() throws Exception{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Get the schema
		String schema = ServletTestHelper.getSchema(dispatchServlet, Project.class, userName);
		assertNotNull(schema);
		log.info("Project schema: "+schema);
	}
	

	
	@Test
	public void testUpdateEntityAcl() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, Project.class, clone.getId(), userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
		acl = ServletTestHelper.updateEntityAcl(dispatchServlet, Project.class, acl, userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
	}
	
	@Test
	public void testCreateEntityAcl() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		
		// create a dataset in the project
		Dataset ds = new Dataset();
		ds.setName("testDataset");
		ds.setParentId(clone.getId());
		Dataset dsClone = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(dsClone);
		toDelete.add(dsClone.getId());
		
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertNotNull(acl);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl.getId());
		assertNotNull(acl.getUri());
		
		// now switch to child
		acl.setId(dsClone.getId());
		AccessControlList childAcl = new AccessControlList();
		childAcl.setId(dsClone.getId());
		childAcl.setResourceAccess(new HashSet<ResourceAccess>());
		// (Is this OK, or do we have to make new ResourceAccess objects inside?)
		// now POST to /dataset/{id}/acl with this acl as the body
		AccessControlList acl2 = ServletTestHelper.createEntityACL(dispatchServlet, Dataset.class, childAcl, userName);
		assertNotNull(acl2.getUri());
		// now retrieve the acl for the child. should get its own back
		AccessControlList acl3 = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertEquals(dsClone.getId(), acl3.getId());
		
		
		// now delete the ACL (restore inheritance)
		ServletTestHelper.deleteEntityACL(dispatchServlet, Dataset.class,  dsClone.getId(), userName);
		// try retrieving the ACL for the child
		
		// should get the parent's ACL
		AccessControlList acl4 = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertNotNull(acl4);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl4.getId());
	}
	
	@Test
	public void testHasAccess() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		
		String userId = userName;
		String accessType = AuthorizationConstants.ACCESS_TYPE.READ.name();
		assertEquals(new BooleanResult(true), ServletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), userId, accessType));
		
		userId = "foo"; // arbitrary user shouldn't have access
		assertEquals(new BooleanResult(false), ServletTestHelper.hasAccess(dispatchServlet, Project.class, clone.getId(), userId, accessType));
	}
	
	/**
	 * This is a test for PLFM-473.
	 * @throws IOException 
	 * @throws ServletException 
	 */
	@Test
	public void testProjectUpdate() throws ServletException, IOException{
		// Frist create a project as a non-admin
		Project project = new Project();
		project.setName("testProjectUpdatePLFM-473");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Now make sure this user can update
		String newName = "testProjectUpdatePLFM-473-updated";
		clone.setName("testProjectUpdatePLFM-473-updated");
		clone = ServletTestHelper.updateEntity(dispatchServlet, clone, TestUserDAO.TEST_USER_NAME);
		clone = ServletTestHelper.getEntity(dispatchServlet, Project.class, clone.getId(),  TestUserDAO.TEST_USER_NAME);
		assertEquals(newName, clone.getName());
		
	}
	

}
