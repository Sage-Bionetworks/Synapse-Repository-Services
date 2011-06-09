package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.HasLayers;
import org.sagebionetworks.repo.model.HasLocations;
import org.sagebionetworks.repo.model.HasPreviews;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * This is a an integration test for the default controller.
 * 
 * @author jmhill
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DefaultControllerAutowiredAllTypesTest {

	// Used for cleanup
	@Autowired
	GenericEntityController entityController;
	
	@Autowired
	public UserManager userManager;

	static private Log log = LogFactory
			.getLog(DefaultControllerAutowiredAllTypesTest.class);

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
		servletConfig.addInitParameter("contextConfigLocation",	"classpath:test-context.xml");
		dispatchServlet = new DispatcherServlet();
		dispatchServlet.init(servletConfig);

	}

	@Test
	public void testCreateAllTypes() throws Exception {
		// For now put each object in a project so their parent id is not null;
		// Create a project
		Project parent = new Project();
		parent.setName("testCreateAllTypesProjectParent");
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, userName);
		assertNotNull(parent);
		toDelete.add(parent.getId());

		// Create one of each type
		ObjectType[] types = ObjectType.values();
		int index = 0;
		for(ObjectType type: types){
			String name = type.name()+index;
			Nodeable object =ObjectTypeFactory.createObjectForTest(name, type, parent.getId());
			Nodeable clone = ServletTestHelper.createEntity(dispatchServlet, object, userName);
			assertNotNull(clone);
			assertNotNull(clone.getId());
			toDelete.add(clone.getId());
			assertNotNull(clone.getEtag());
			// Check the base ursl
			UrlHelpers.validateAllUrls(clone);
			index++;
		}
	}



	@Test
	public void testGetById() throws Exception {
		// For now put each object in a project so their parent id is not null;
		// Create a project
		Project parent = new Project();
		parent.setName("testCreateAllTypesProjectParent");
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, userName);
		assertNotNull(parent);
		toDelete.add(parent.getId());

		// Create one of each type
		ObjectType[] types = ObjectType.values();
		int index = 0;
		for(ObjectType type: types){
			String name = type.name()+index;
			Nodeable object =ObjectTypeFactory.createObjectForTest(name, type, parent.getId());
			Nodeable clone = ServletTestHelper.createEntity(dispatchServlet, object, userName);
			assertNotNull(clone);
			assertNotNull(clone.getId());
			toDelete.add(clone.getId());
			assertNotNull(clone.getEtag());
			// Check the base ursl
			UrlHelpers.validateAllUrls(clone);
			
			// Now get the project object
			Nodeable fromGet = ServletTestHelper.getEntity(dispatchServlet, type.getClassForType(), clone.getId(), userName);
			assertNotNull(fromGet);
			// Should match the clone
			assertEquals(clone, fromGet);
			// Now get the object
			index++;
		}
	}

	@Test
	public void testGetList() throws Exception {
		
		// For now put each object in a project so their parent id is not null;
		// Create a project
		Project parent = new Project();
		parent.setName("testCreateAllTypesProjectParent");
		parent = ServletTestHelper.createEntity(dispatchServlet, parent, userName);
		assertNotNull(parent);
		toDelete.add(parent.getId());
		
		// Create 3 of each type
		int number = 3;
		for (int i = 0; i < number; i++) {
			// Create one of each type
			ObjectType[] types = ObjectType.values();
			int index = 0;
			for(ObjectType type: types){
				String name = type.name()+index;
				Nodeable object =ObjectTypeFactory.createObjectForTest(name, type, parent.getId());
				Nodeable clone = ServletTestHelper.createEntity(dispatchServlet, object, userName);
				assertNotNull(clone);
				assertNotNull(clone.getId());
				toDelete.add(clone.getId());
				assertNotNull(clone.getEtag());
				// Check the base ursl
				UrlHelpers.validateAllUrls(clone);
				index++;
			}
		}
		
		ObjectType[] types = ObjectType.values();
		int index = 0;
		for(ObjectType type: types){
			// Try with all default values
			PaginatedResults<Nodeable> result = ServletTestHelper.getAllEntites(dispatchServlet, type.getClassForType(), null, null, null, null, userName);
			assertNotNull(result);
			int expectedNumer = number;
			if(ObjectType.project == type){
				// There is one extra project since we use that as the parent.
				expectedNumer++;
			}
			assertEquals(expectedNumer, result.getTotalNumberOfResults());
			assertNotNull(result.getResults());
			assertEquals(expectedNumer, result.getResults().size());

			// Try with a value in each slot
			result = ServletTestHelper.getAllEntites(dispatchServlet, type.getClassForType(), 2, 1,	"name", true, userName);
			assertNotNull(result);
			assertEquals(expectedNumer, result.getTotalNumberOfResults());
			assertNotNull(result.getResults());
			assertEquals(1, result.getResults().size());
			assertNotNull(result.getResults().get(0));
		}
	}
	
	@Test
	public void testEntityChildren() throws Exception {
		// Create a project
		Project root = new Project();
		root.setName("projectRoot");
		root = ServletTestHelper.createEntity(dispatchServlet, root, userName);
		assertNotNull(root);
		toDelete.add(root.getId());
		
		// Create one of each type
		ObjectType[] types = ObjectType.values();
		for(ObjectType parent: types){
			for(ObjectType child: types){
				// First create a parent of this type
				String name = "parent_"+parent.name()+"Ofchild_"+child.name();
				Nodeable parentObject =ObjectTypeFactory.createObjectForTest(name, parent, root.getId());
				parentObject = ServletTestHelper.createEntity(dispatchServlet, parentObject, userName);
				assertNotNull(parentObject);
				toDelete.add(parentObject.getId());
				
				// Create two children of this node.
				for(int i=0; i<2; i++){
					name = "child_"+child.name()+"OfParent_"+parent.name();
					// Create this as a child of the parent
					Nodeable childObject = ObjectTypeFactory.createObjectForTest(name, child, parentObject.getId());
					childObject = ServletTestHelper.createEntity(dispatchServlet, childObject, userName);
					assertNotNull(childObject);
					toDelete.add(parentObject.getId());
				}
				// Now get all children of this parent
				PaginatedResults<Nodeable> results = ServletTestHelper.getAllChildrenEntites(dispatchServlet, parent, parentObject.getId(), child.getClassForType(), 1, 100, "name", true, userName);
				assertNotNull(results);
				assertEquals("Parent: "+parent.name()+" with child: "+child.name(), 2, results.getTotalNumberOfResults());
				assertNotNull(results.getResults());
				assertEquals(2, results.getResults().size());
			}
		}
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
		project = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(project);
		toDelete.add(project.getId());
		ObjectType[] types = ObjectType.values();
		int index = 0;
		for(ObjectType type: types){
			// Get the schema for each type and confirm it matches this object
			String schema = ServletTestHelper.getSchema(dispatchServlet, type.getClassForType(), userName);
			assertNotNull(schema);
			validateSchemaForObject(schema, type);
		}
	}
	/**
	 * Helper to validate a schema for an ojbect
	 * @param schema
	 * @param type
	 * @throws JSONException
	 */
	private void validateSchemaForObject(String schema, ObjectType type) throws JSONException{
		// The schema should contain all fields of this 
		JSONObject objectFromSchema = new JSONObject(schema);
		JSONObject properties = objectFromSchema.getJSONObject("properties");
		assertNotNull(properties);
		Field[] fields = type.getClassForType().getDeclaredFields();
		// The schema should have each field name a s key
		for(Field field: fields){
			assertTrue("expected key: "+field.getName(),properties.has(field.getName()));
		}
	}
	
	@Test
	public void testUpdateEntity() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Now change the name
		clone.setName("my new project name");
		Project updated = ServletTestHelper.updateEntity(dispatchServlet, clone, userName);
		assertNotNull(updated);
		// It should have a new etag
		assertNotNull(updated.getEtag());
		assertFalse(updated.getEtag().equals(clone.getEtag()));
		// Now get the project object
		Project fromGet = ServletTestHelper.getEntity(dispatchServlet, Project.class, clone.getId(), userName);
		assertEquals(updated, fromGet);
		assertEquals("my new project name", fromGet.getName());
	}
	
	@Test
	public void testGetAnnotations() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Make sure we can get the annotations for this entity.
		Annotations annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, Project.class, clone.getId(), userName);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
	}
	
	@Test
	public void testUpdateAnnotations() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		// Make sure we can get the annotations for this entity.
		Annotations annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, Project.class, clone.getId(), userName);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		annos.addAnnotation("someStringKey", "one");
		annos.addAnnotation("someBlobKey", "I am a very long string".getBytes("UTF-8"));
		// Do the update
		Annotations updatedAnnos = ServletTestHelper.updateEntityAnnotations(dispatchServlet, Project.class, annos, userName);
		assertNotNull(updatedAnnos);
		assertNotNull(updatedAnnos.getEtag());
		assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));
		assertEquals("one", updatedAnnos.getSingleValue("someStringKey"));
		assertNotNull(updatedAnnos.getBlobAnnotations().get("someBlobKey"));
	}
	
	@Test
	public void testGetEntityAcl() throws ServletException, IOException{
		// Create a project
		Project project = new Project();
		project.setName("testCreateProject");
		Project clone = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(clone);
		toDelete.add(clone.getId());
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, Project.class, clone.getId(), userName);
		assertNotNull(acl);
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
		ServletTestHelper.updateEntityAcl(dispatchServlet, Project.class, acl, userName);
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
		assertEquals(clone.getId(), acl.getResourceId());
		
		// now switch to child
		acl.setResourceId(dsClone.getId());
		acl.setId(null);
		// (Is this OK, or do we have to make new ResourceAccess objects inside?)
		// now POST to /dataset/{id}/acl with this acl as the body
		AccessControlList acl2 = ServletTestHelper.createEntityACL(dispatchServlet, Dataset.class, acl, userName);
		// now retrieve the acl for the child. should get its own back
		AccessControlList acl3 = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertEquals(dsClone.getId(), acl3.getResourceId());
		
		
		// now delete the ACL (restore inheritance)
		ServletTestHelper.deleteEntityACL(dispatchServlet, Dataset.class,  dsClone.getId(), userName);
		// try retrieving the ACL for the child
		
		// should get the parent's ACL
		AccessControlList acl4 = ServletTestHelper.getEntityACL(dispatchServlet, Dataset.class, dsClone.getId(), userName);
		assertNotNull(acl4);
		// the returned ACL should refer to the parent
		assertEquals(clone.getId(), acl4.getResourceId());
	}
	

	

}
