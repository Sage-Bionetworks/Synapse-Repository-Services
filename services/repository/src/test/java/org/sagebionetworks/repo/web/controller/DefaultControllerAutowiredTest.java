package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.ObjectType;
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
	public void testUpdateEntityAcl() throws ServletException, IOException, ACLInheritanceException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, Project.class, clone.getId(), userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
		acl = ServletTestHelper.updateEntityAcl(dispatchServlet, Project.class, clone.getId(), acl, userName);
		assertNotNull(acl);
		assertNotNull(acl.getUri());
	}
	
	@Test
	public void testCreateEntityAcl() throws ServletException, IOException, ACLInheritanceException{
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
		
		AccessControlList acl = null;
		try {
			acl = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
			fail("Should have failed to get the ACL of an inheriting node");
		} catch (ACLInheritanceException e) {
			// Get the ACL from the redirect
			acl = ServletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorType().getClassForType(), e.getBenefactorId(), userName);
		}
		assertNotNull(acl);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl.getId());
		assertNotNull(acl.getUri());
		
		// now switch to child

		acl.setId(null);
		AccessControlList childAcl = new AccessControlList();
		// We should be able to start with a null ID per PLFM-410
		childAcl.setId(null);
		childAcl.setResourceAccess(new HashSet<ResourceAccess>());
		// (Is this OK, or do we have to make new ResourceAccess objects inside?)
		// now POST to /dataset/{id}/acl with this acl as the body
		AccessControlList acl2 = ServletTestHelper.createEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), childAcl, userName);
		assertNotNull(acl2.getUri());
		// now retrieve the acl for the child. should get its own back
		AccessControlList acl3 = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertEquals(dsClone.getId(), acl3.getId());
		
		
		// now delete the ACL (restore inheritance)
		ServletTestHelper.deleteEntityACL(dispatchServlet, Dataset.class,  dsClone.getId(), userName);
		// try retrieving the ACL for the child
		
		// should get the parent's ACL
		AccessControlList acl4 = null;
		try{
			 ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		}catch (ACLInheritanceException e){
			acl4 = ServletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorType().getClassForType(), e.getBenefactorId(), userName);
		}

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
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		assertEquals("The name should match the ID when the name is set to null", clone.getId(), clone.getName());
		// Now make sure this user can update
		String newName = "testProjectUpdatePLFM-473-updated";
		clone.setName("testProjectUpdatePLFM-473-updated");
		clone = ServletTestHelper.updateEntity(dispatchServlet, clone, TestUserDAO.TEST_USER_NAME);
		clone = ServletTestHelper.getEntity(dispatchServlet, Project.class, clone.getId(),  TestUserDAO.TEST_USER_NAME);
		assertEquals(newName, clone.getName());
		
	}
	
	/**
	 * This is a test for PLFM-431.  If you try to get an object where they type does not match the ID
	 * an exception should be thrown.
	 * @throws IOException 
	 * @throws ServletException 
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testTypeDoesNotMatchId() throws ServletException, IOException{
		// First create a project as a non-admin
		Project project = new Project();
		// Make sure we can still set a name to null.  The name should then match the ID.
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Now try to get the project as a dataset
		Object wrong = ServletTestHelper.getEntity(dispatchServlet, Dataset.class, clone.getId(),  TestUserDAO.TEST_USER_NAME);
		
	}
	
	@Test
	public void testGetEntityType() throws ServletException, IOException{
		Project project = new Project();
		project.setName(null);
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		
		EntityHeader type = ServletTestHelper.getEntityType(dispatchServlet, clone.getId(), TestUserDAO.TEST_USER_NAME);
		assertNotNull(type);
		assertEquals(ObjectType.project.getUrlPrefix(), type.getType());
		assertEquals(clone.getId(), type.getName());
		assertEquals(clone.getId(), type.getId());
	}
	
	@Test
	public void testGetEntityBenefactor() throws ServletException, IOException{
		Project project = new Project();
		project.setName(null);
		project = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);;
		toDelete.add(project.getId());
		// Create a dataset
		Dataset ds = new Dataset();
		ds.setParentId(project.getId());
		ds = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		
		// Now get the permission information for the project
		EntityHeader benefactor = ServletTestHelper.getEntityBenefactor(dispatchServlet, project.getId(), Project.class, TestUserDAO.TEST_USER_NAME);
		assertNotNull(benefactor);
		// The project should be its own benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(ObjectType.project.getUrlPrefix(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());
		
		// Now check the dataset
		benefactor = ServletTestHelper.getEntityBenefactor(dispatchServlet, ds.getId(), Dataset.class, TestUserDAO.TEST_USER_NAME);
		assertNotNull(benefactor);
		// The project should be the dataset's benefactor
		assertEquals(project.getId(), benefactor.getId());
		assertEquals(ObjectType.project.getUrlPrefix(), benefactor.getType());
		assertEquals(project.getName(), benefactor.getName());
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAclUpdateWithChildType() throws ServletException, IOException, ACLInheritanceException{
		Project project = new Project();
		project.setName(null);
		project = ServletTestHelper.createEntity(dispatchServlet, project, TestUserDAO.TEST_USER_NAME);;
		toDelete.add(project.getId());
		// Create a dataset
		Dataset ds = new Dataset();
		ds.setParentId(project.getId());
		ds = ServletTestHelper.createEntity(dispatchServlet, ds, userName);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		
		// Get the ACL for the project
		AccessControlList projectAcl = ServletTestHelper.getEntityACL(dispatchServlet, Project.class, project.getId(), TestUserDAO.TEST_USER_NAME);
		
		// Now attempt to update the ACL as the dataset.
		projectAcl = ServletTestHelper.updateEntityAcl(dispatchServlet, Dataset.class, ds.getId(), projectAcl, TestUserDAO.TEST_USER_NAME);
		
	}

}
