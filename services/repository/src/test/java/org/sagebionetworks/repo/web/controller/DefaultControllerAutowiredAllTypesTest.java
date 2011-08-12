package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Nodeable;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.Versionable;
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
	public void testAnonymousGet() throws ServletException, IOException{
		Project project = new Project();
		project.setName("testAnonymousGet");
		project = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		String id = project.getId();
		assertNotNull(project);
		toDelete.add(id);
		// Grant this project public access
		AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, Project.class, id, userName);
		assertNotNull(acl);
		assertEquals(id, acl.getId());
		ResourceAccess ac = new ResourceAccess();
		ac.setGroupName(AuthorizationConstants.DEFAULT_GROUPS.PUBLIC.name());
		ac.setAccessType(new HashSet<ACCESS_TYPE>());
		ac.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ac);
		ServletTestHelper.updateEntityAcl(dispatchServlet, Project.class, acl, userName);
		
		// Make sure the anonymous user can see this.
		Project clone = ServletTestHelper.getEntity(dispatchServlet, Project.class, project.getId(), AuthorizationConstants.ANONYMOUS_USER_ID);
		assertNotNull(clone);
	}
	
	/**
	 * This is a test helper method that will create at least on of each type of entity.
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws InvalidModelException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	private List<Nodeable> createEntitesOfEachType(int countPerType) throws ServletException, IOException, InstantiationException, IllegalAccessException, InvalidModelException, DatastoreException, NotFoundException, UnauthorizedException{
		// For now put each object in a project so their parent id is not null;
		// Create a project
		Project project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = ServletTestHelper.createEntity(dispatchServlet, project, userName);
		assertNotNull(project);
		toDelete.add(project.getId());
		// Create a dataset
		Dataset datasetParent = (Dataset) ObjectTypeFactory.createObjectForTest("datasetParent", ObjectType.dataset, project.getId());
		datasetParent = ServletTestHelper.createEntity(dispatchServlet, datasetParent, userName);
		// Create a layer parent
		InputDataLayer layerParent = (InputDataLayer) ObjectTypeFactory.createObjectForTest("layerParent", ObjectType.layer, datasetParent.getId());
		layerParent = ServletTestHelper.createEntity(dispatchServlet, layerParent, userName);
		// Now get the path of the layer
		List<EntityHeader> path = entityController.getEntityPath(userName, layerParent.getId());
		
		// This is the list of entities that will be created.
		List<Nodeable> newChildren = new ArrayList<Nodeable>();
		// Create one of each type
		ObjectType[] types = ObjectType.values();
		for(int i=0; i<countPerType; i++){
			int index = i;
			Dataset dataset = null;
			Eula eula = null;
			for(ObjectType type: types){
				String name = type.name()+index;
				// use the correct parent type.
				String parentId = findCompatableParentId(path, type);
				Nodeable object = ObjectTypeFactory.createObjectForTest(name, type, parentId);
				if (ObjectType.agreement == type) {
					// Dev Note: we are depending upon the fact that in the
					// object type enumeration, agreement comes after dataset
					// and eula
					Agreement agreement = (Agreement) object;
					agreement.setDatasetId(dataset.getId());
					agreement.setEulaId(eula.getId());
				}
				Nodeable clone = ServletTestHelper.createEntity(dispatchServlet, object, userName);
				assertNotNull(clone);
				assertNotNull(clone.getId());
				assertNotNull(clone.getEtag());
				if(parentId == null){
					// We need to delete any node that does not have a parent
					toDelete.add(clone.getId());
				}
				
				// Stash these for later use
				if (ObjectType.dataset == type) {
					dataset = (Dataset) clone;
				} else if (ObjectType.eula == type) {
					eula = (Eula) clone;
				} 
				
				// Check the base ursl
				UrlHelpers.validateAllUrls(clone);
				// Add this to the list of entities created
				newChildren.add(clone);
				index++;
			}
		}
		return newChildren;
	}
	
	/**
	 * Find the first compatible parent id for a given object type. 
	 * @param path
	 * @param type
	 * @return
	 */
	private String findCompatableParentId(List<EntityHeader> path, ObjectType type){
		// Frist try null
		if(type.isValidParentType(null)) return null;
		// Try each entry in the list
		for(EntityHeader header: path){
			ObjectType parentType = ObjectType.getFirstTypeInUrl(header.getType());
			if(type.isValidParentType(parentType)){
				return header.getId();
			}
		}
		// No match found
		throw new IllegalArgumentException("Cannot find a compatible parent for "+type);
	}

	@Test
	public void testCreateAllTypes() throws Exception {
		// All we need to do is create at least one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
	}



	@Test
	public void testGetById() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now make sure we can get each type
		for(Nodeable entity: created){
			// Can we get it?
			Nodeable fromGet = ServletTestHelper.getEntity(dispatchServlet,entity.getClass(), entity.getId(), userName);
			assertNotNull(fromGet);
			// Should match the clone
			assertEquals(entity, fromGet);
		}
	}
	
	@Test
	public void testGetList() throws Exception {
		// This time we want 3 of each type.
		int number = 3;
		List<Nodeable> created = createEntitesOfEachType(number);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		ObjectType[] types = ObjectType.values();
		int index = 0;
		for(ObjectType type: types){
			if (ObjectType.agreement == type) continue;
			// Try with all default values
			PaginatedResults<Nodeable> result = ServletTestHelper.getAllEntites(dispatchServlet, type.getClassForType(), null, null, null, null, userName);
			assertNotNull(result);
			int expectedNumer = number;
			if(ObjectType.project == type || ObjectType.dataset == type || ObjectType.layer == type){
				// There is one extra project since we use that as the parent.
				expectedNumer++;
			}
			if(ObjectType.folder == type){
				// There are three root folders
				expectedNumer = expectedNumer+3;
			}
			assertTrue(result.getTotalNumberOfResults() >= expectedNumer );
			assertNotNull(result.getResults());
			assertTrue(result.getResults().size() >= expectedNumer);

			// Try with a value in each slot
			result = ServletTestHelper.getAllEntites(dispatchServlet, type.getClassForType(), 2, 1,	"name", true, userName);
			assertNotNull(result);
			assertTrue(result.getTotalNumberOfResults() >= expectedNumer );
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
		
		// Create a dataset
		Dataset datasetParent = (Dataset) ObjectTypeFactory.createObjectForTest("datasetParent", ObjectType.dataset, root.getId());
		datasetParent = ServletTestHelper.createEntity(dispatchServlet, datasetParent, userName);
		// Create a layer parent
		InputDataLayer layerParent = (InputDataLayer) ObjectTypeFactory.createObjectForTest("layerParent", ObjectType.layer, datasetParent.getId());
		layerParent = ServletTestHelper.createEntity(dispatchServlet, layerParent, userName);
		// Now get the path of the layer
		List<EntityHeader> path = entityController.getEntityPath(userName, layerParent.getId());
		
		// Create one of each type
		ObjectType[] types = ObjectType.values();
		int count = 0;
		for(ObjectType parent: types){
			if(ObjectType.agreement == parent) continue;
			for(ObjectType child: types){
				if(ObjectType.agreement == child) continue;
				if(ObjectType.agreement == child) continue;
				// Only test valid parent child combinations
				if(child.isValidParentType(parent)){
					// First create a parent of this type
					String name = "parent_"+parent.name()+"Ofchild_"+child.name();
					String parentId = findCompatableParentId(path, parent);
					Nodeable parentObject = ObjectTypeFactory.createObjectForTest(name, parent, parentId);
					parentObject = ServletTestHelper.createEntity(dispatchServlet, parentObject, userName);
					assertNotNull(parentObject);
					toDelete.add(parentObject.getId());
					
					// Create two children of this node.
					for(int i=0; i<2; i++){
						name = "child_"+child.name()+"OfParent_"+parent.name()+count;
						// Create this as a child of the parent
						Nodeable childObject = ObjectTypeFactory.createObjectForTest(name, child, parentObject.getId());
						childObject = ServletTestHelper.createEntity(dispatchServlet, childObject, userName);
						assertNotNull(childObject);
						toDelete.add(parentObject.getId());
						count++;
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
	}

	@Test(expected = NotFoundException.class)
	public void testDelete() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now delete each one
		for(Nodeable entity: created){
			ServletTestHelper.deleteEntity(dispatchServlet, entity.getClass(), entity.getId(), userName);
			// This should throw an exception
			HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
			entityController.getEntity(userName, entity.getId(), mockRequest, Project.class);
		}
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
	public void testUpdateEntity() throws Exception{
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		int counter=0;
		for(Nodeable entity: created){
			// Now change the name
			String newName ="my new name"+counter;
			entity.setName(newName);
			Nodeable updated = ServletTestHelper.updateEntity(dispatchServlet, entity, userName);
			assertNotNull(updated);
			// Updating an entity should not create a new version
			if(updated instanceof Versionable){
				Versionable updatedVersionable = (Versionable) updated;
				assertEquals(new Long(1), updatedVersionable.getVersionNumber());
			}
			// It should have a new etag
			assertNotNull(updated.getEtag());
			assertFalse(updated.getEtag().equals(entity.getEtag()));
			// Now get the object
			Nodeable fromGet = ServletTestHelper.getEntity(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertEquals(updated, fromGet);
			assertEquals(newName, fromGet.getName());
			counter++;
		}
	}
	
	@Test
	public void testGetPath() throws Exception{
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			// Make sure we can get the annotations for this entity.
			List<EntityHeader> path = ServletTestHelper.getEntityPath(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(path);
			assertTrue(path.size() > 0);
			ObjectType type = ObjectType.getNodeTypeForClass(entity.getClass());
			// The last element should match this entity
			EntityHeader myData = path.get(path.size()-1);
			assertNotNull(myData);
			assertEquals(entity.getId(), myData.getId());
			assertEquals(entity.getName(), myData.getName());
			assertEquals(type.getUrlPrefix(), myData.getType());
		}
	}
	
	@Test
	public void testGetAnnotations() throws Exception{
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			// Make sure we can get the annotations for this entity.
			Annotations annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(annos);
			// Annotations use the same etag as the entity
			assertEquals(entity.getEtag(), annos.getEtag());
			// Annotations use the same id as the entity
			assertEquals(entity.getId(), annos.getId());
		}
	}
	
	@Test
	public void testUpdateAnnotations() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			// Make sure we can get the annotations for this entity.
			Annotations annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(annos);
			assertNotNull(annos.getEtag());
			annos.addAnnotation("someStringKey", "one");
			annos.addAnnotation("someBlobKey", "I am a very long string".getBytes("UTF-8"));
			// Do the update
			Annotations updatedAnnos = ServletTestHelper.updateEntityAnnotations(dispatchServlet, entity.getClass(), annos, userName);
			assertNotNull(updatedAnnos);
			assertNotNull(updatedAnnos.getEtag());
			assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));
			assertEquals("one", updatedAnnos.getSingleValue("someStringKey"));
			assertNotNull(updatedAnnos.getBlobAnnotations().get("someBlobKey"));
		}

	}
	
	@Test
	public void testGetEntityAcl() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(acl);
		}
	}
	
	@Test
	public void testUpdateEntityAcl() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(acl);
			ServletTestHelper.updateEntityAcl(dispatchServlet, Project.class, acl, userName);
		}

	}
	
	@Test
	public void testCreateEntityAcl() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		
		// Now update each
		for(Nodeable entity: created){
			Nodeable child = (Nodeable)entity;
			AccessControlList acl = ServletTestHelper.getEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(acl);
			// Get the full path of this entity.
			List<EntityHeader> path = ServletTestHelper.getEntityPath(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(path);
			assertTrue(path.size() > 0);
			// The ACL should match the root of the node
			EntityHeader rootHeader = path.get(1);
			// the returned ACL should refer to the parent
			assertEquals(rootHeader.getId(), acl.getId());
			
			// We cannot add an ACL to a node that already has one
			if(acl.getId().equals(entity.getId())){
				continue;
			}
			
			// now switch to child
			acl.setId(entity.getId());
			// (Is this OK, or do we have to make new ResourceAccess objects inside?)
			// now POST to /dataset/{id}/acl with this acl as the body
			AccessControlList acl2 = ServletTestHelper.createEntityACL(dispatchServlet, entity.getClass(), acl, userName);
			// now retrieve the acl for the child. should get its own back
			AccessControlList acl3 = ServletTestHelper.getEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertEquals(entity.getId(), acl3.getId());
			
			
			// now delete the ACL (restore inheritance)
			ServletTestHelper.deleteEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			// try retrieving the ACL for the child
			
			// should get the parent's ACL
			AccessControlList acl4 = ServletTestHelper.getEntityACL(dispatchServlet, entity.getClass(), entity.getId(), userName);
			assertNotNull(acl4);
			// the returned ACL should refer to the parent
			assertEquals(rootHeader.getId(), acl4.getId());
		}
	}
	
	@Test
	public void testCreateNewVersion() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		// Now update each
		for(Nodeable entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof Versionable){
				Versionable versionableEntity = (Versionable) entity;
				// Before we start, make sure there is only one version so far
				assertEquals(new Long(1), versionableEntity.getVersionNumber());
				assertNotNull(versionableEntity.getVersionLabel());
				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity.setVersionLabel("1.1.99");
				versionableEntity.setVersionComment("Testing the DefaultController.createNewVersion()");
				Versionable newVersion = ServletTestHelper.createNewVersion(dispatchServlet, versionableEntity, userName);
				assertNotNull(newVersion);
				// Make sure we have a new version number.
				assertEquals(new Long(2), newVersion.getVersionNumber());
				assertEquals(versionableEntity.getVersionLabel(), newVersion.getVersionLabel());
				assertEquals(versionableEntity.getVersionComment(), newVersion.getVersionComment());
			}
		}
	}
	
	@Test
	public void testGetEntityForVersion() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		// Now update each
		for(Nodeable entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof Versionable){
				Versionable versionableEntity = (Versionable) entity;

				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity.setVersionLabel("1.1.99");
				versionableEntity.setVersionComment("Testing the DefaultController.testGetVersion()");
				Versionable newVersion = ServletTestHelper.createNewVersion(dispatchServlet, versionableEntity, userName);
				assertNotNull(newVersion);
				// Make sure we have a new version number.
				assertEquals(new Long(2), newVersion.getVersionNumber());
				assertEquals(versionableEntity.getVersionLabel(), newVersion.getVersionLabel());
				assertEquals(versionableEntity.getVersionComment(), newVersion.getVersionComment());
				
				// Get the first version
				Versionable v1 =ServletTestHelper.getEntityForVersion(dispatchServlet, versionableEntity.getClass(), versionableEntity.getId(), new Long(1), userName);
				assertNotNull(v1);
				assertEquals(new Long(1), v1.getVersionNumber());
				UrlHelpers.validateAllUrls(v1);
				// now get the second version
				Versionable v2 =ServletTestHelper.getEntityForVersion(dispatchServlet, versionableEntity.getClass(), versionableEntity.getId(), new Long(2), userName);
				assertNotNull(v2);
				assertEquals(new Long(2), v2.getVersionNumber());
				UrlHelpers.validateAllUrls(v2);
			}
		}
	}
	
	@Test
	public void testGetAllVersions() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		// Now update each
		int numberVersion = 4;
		for(Nodeable entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof Versionable){
				Versionable versionableEntity = (Versionable) entity;
				// Create multiple versions for each.
				for(int i=0; i<numberVersion; i++){
					// Create a comment and label for each
					versionableEntity = ServletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), userName);
					versionableEntity.setVersionLabel("1.1."+i);
					versionableEntity.setVersionComment("Comment: "+i);
					ServletTestHelper.createNewVersion(dispatchServlet, versionableEntity, userName);
				}
				long currentVersion = numberVersion+1;
				long previousVersion = currentVersion-1;
				long firstVersion = 1;
				// Now get all entities
				PaginatedResults<Versionable> results = ServletTestHelper.getAllVersionsOfEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), 1, 100, userName);
				assertNotNull(results);
				assertEquals(currentVersion, results.getTotalNumberOfResults());
				assertNotNull(results.getResults());
				assertEquals(currentVersion, results.getResults().size());
				// The first should be the current version
				assertNotNull(results.getResults().get(0));
				assertEquals(new Long(currentVersion), results.getResults().get(0).getVersionNumber());
				// The last should be the first version
				assertEquals(new Long(firstVersion), results.getResults().get(results.getResults().size()-1).getVersionNumber());
				
				// Query again but this time get a sub-set
				results = ServletTestHelper.getAllVersionsOfEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), 2, 3, userName);
				assertNotNull(results);
				assertEquals(currentVersion, results.getTotalNumberOfResults());
				assertNotNull(results.getResults());
				assertEquals(3, results.getResults().size());
				// The first should be the previous version
				assertNotNull(results.getResults().get(0));
				assertEquals(new Long(previousVersion), results.getResults().get(0).getVersionNumber());
				// The last should be the previous version - 2;
				assertEquals(new Long(previousVersion-2), results.getResults().get(results.getResults().size()-1).getVersionNumber());
			}
		}
	}
	
	@Test
	public void testGetEntityAnnotationsForVersion() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		// Now update each
		for(Nodeable entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof Versionable){
				Versionable versionableEntity = (Versionable) entity;
				
				// Before we create a new version make sure the current version has some annotations
				Annotations v1Annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, versionableEntity.getClass(), entity.getId(), userName);
				assertNotNull(v1Annos);
				String v1Value = "I am on the first version, whooo hooo!...";
				v1Annos.addAnnotation("stringKey", v1Value);
				v1Annos = ServletTestHelper.updateEntityAnnotations(dispatchServlet, versionableEntity.getClass(), v1Annos, userName);

				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity = ServletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), userName);
				versionableEntity.setVersionLabel("1.1.80");
				versionableEntity.setVersionComment("Testing the DefaultController.EntityAnnotationsForVersion()");
				Versionable newVersion = ServletTestHelper.createNewVersion(dispatchServlet, versionableEntity, userName);
				assertNotNull(newVersion);
				
				// Make sure the new version has the annotations
				Annotations v2Annos = ServletTestHelper.getEntityAnnotations(dispatchServlet, versionableEntity.getClass(), entity.getId(), userName);
				assertNotNull(v2Annos);
				assertEquals(v1Value, v2Annos.getSingleValue("stringKey"));
				// Now update the v2 annotations
				v2Annos.getStringAnnotations().clear();
				String v2Value = "I am on the second version, booo hooo!...";
				v2Annos.addAnnotation("stringKey", v2Value);
				v2Annos = ServletTestHelper.updateEntityAnnotations(dispatchServlet, versionableEntity.getClass(), v2Annos, userName);
				
				// Now make sure we can get both v1 and v2 annotations and each has the correct values
				//v1
				v1Annos = ServletTestHelper.getEntityAnnotationsForVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 1l, userName);
				assertNotNull(v1Annos);
				assertEquals(v1Value, v1Annos.getSingleValue("stringKey"));
				//v2
				v2Annos = ServletTestHelper.getEntityAnnotationsForVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 2l, userName);
				assertNotNull(v2Annos);
				assertEquals(v2Value, v2Annos.getSingleValue("stringKey"));
			}
		}
	}
	
	@Test
	public void testDeleteVersion() throws Exception {
		// First create one of each type
		List<Nodeable> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= ObjectType.values().length);
		// Now update each
		for(Nodeable entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof Versionable){
				Versionable versionableEntity = (Versionable) entity;
				
				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity = ServletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), userName);
				versionableEntity.setVersionLabel("1.1.80");
				versionableEntity.setVersionComment("Testing the DefaultController.testDeleteVersion()");
				Versionable newVersion = ServletTestHelper.createNewVersion(dispatchServlet, versionableEntity, userName);
				assertNotNull(newVersion);
				
				// There should be two versions
				PaginatedResults<Versionable> paging = ServletTestHelper.getAllVersionsOfEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), 1, 100, userName);
				assertNotNull(paging);
				assertEquals(2, paging.getTotalNumberOfResults());
				
				// Now delete the new version
				ServletTestHelper.deleteEntityVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 2l, userName);
				// We should be down to one version
				paging = ServletTestHelper.getAllVersionsOfEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), 1, 100, userName);
				assertNotNull(paging);
				assertEquals(1, paging.getTotalNumberOfResults());
				
			}
		}
	}

}
