package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.TypeChangeRequest;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" }, loader =MockWebApplicationContextLoader.class)
@MockWebApplication
public class EntityControllerTest {

	@Autowired
	private EntityServletTestHelper entityServletHelper;
	
	private static final String TEST_USER1 = TestUserDAO.TEST_USER_NAME;
	
	private List<String> toDelete = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(entityServletHelper != null && toDelete != null){
			for(String id: toDelete){
				try {
					entityServletHelper.deleteEntity(id, TEST_USER1);
				} catch (Exception e) {
					// Try even if it fails.
				}
			}
		}
	}
	
	@Test
	public void testCRUDEntity() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		String id = clone.getId();
		toDelete.add(id);
		assertEquals(p.getName(), clone.getName());
		// Now get the entity with the ID
		Project clone2 = (Project) entityServletHelper.getEntity(id, TEST_USER1);
		assertEquals(clone, clone2);
		// Make sure we can update it
		clone2.setName("My new name");
		Project clone3 = (Project) entityServletHelper.updateEntity(clone2, TEST_USER1);
		assertNotNull(clone3);		
		assertEquals(clone2.getName(), clone3.getName());
		// Should not match the original
		assertFalse(p.getName().equals(clone3.getName()));
		// the Etag should have changed
		assertFalse(clone2.getEtag().equals(clone3.getEtag()));
		// Now delete it
		entityServletHelper.deleteEntity(id, TEST_USER1);
		// it should not be found now
		try{
			entityServletHelper.getEntity(id, TEST_USER1);
			fail("Delete failed");
		}catch (NotFoundException e) {
			// expected
		}
	}
	
	@Test
	public void testAnnotationsCRUD() throws Exception{
		Project p = new Project();
		p.setName("AnnotCrud");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		Annotations annos = entityServletHelper.getEntityAnnotaions(id, TEST_USER1);
		assertNotNull(annos);
		// Change the values
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		// Updte them
		Annotations annosClone = entityServletHelper.updateAnnotations(annos, TEST_USER1);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = (String) annosClone.getSingleValue("string");
		assertEquals("A string", value);
		assertEquals(new Double(45.0001), annosClone.getSingleValue("doubleAnno"));
		
	}
	
	@Test
	public void testGetUserEntityPermissions() throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException{
		Project p = new Project();
		p.setName("UserEntityPermissions");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		String id = clone.getId();
		toDelete.add(id);
		UserEntityPermissions uep = entityServletHelper.getUserEntityPermissions(id, TEST_USER1);
		assertNotNull(uep);
		assertTrue(uep.getCanEdit());
	}
	
	@Test
	public void testEntityTypeBatch() throws Exception {
		List<String> ids = new ArrayList<String>();
		for(int i = 0; i < 12; i++) {
			Project p = new Project();
			p.setName("EntityTypeBatchItem" + i);
			p.setEntityType(p.getClass().getName());
			Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1);
			String id = clone.getId();
			toDelete.add(id);
			ids.add(id);
		}
	
		BatchResults<EntityHeader> results = entityServletHelper.getEntityTypeBatch(ids, TEST_USER1);
		assertNotNull(results);
		assertEquals(12, results.getTotalNumberOfResults());
		List<String> outputIds = new ArrayList<String>();
		for(EntityHeader header : results.getResults()) {
			outputIds.add(header.getId());
		}
		assertEquals(ids.size(), outputIds.size());
		assertTrue(ids.containsAll(outputIds));
	}
	
	@Test
	public void testEntityPath() throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException{
		Project p = new Project();
		p.setName("EntityPath");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		String id = clone.getId();
		toDelete.add(id);
		EntityPath path = entityServletHelper.getEntityPath(id, TEST_USER1);
		assertNotNull(path);
		assertNotNull(path.getPath());
		assertEquals(2, path.getPath().size());
		EntityHeader header = path.getPath().get(1);
		assertNotNull(header);
		assertEquals(id, header.getId());
	}
	
	@Test
	public void testGetRESTResources() throws ServletException, IOException, JSONObjectAdapterException{
		RestResourceList rrl = entityServletHelper.getRESTResources();
		assertNotNull(rrl);
		assertNotNull(rrl.getList());
		assertTrue(rrl.getList().size() > 0);
	}
	
	
	@Test
	public void testGetEffectiveSchema() throws ServletException, IOException, JSONObjectAdapterException{
		String resourceId = Study.class.getName();
		ObjectSchema effective = entityServletHelper.getEffectiveSchema(Study.class.getName());
		assertNotNull(effective);
		assertEquals(resourceId, effective.getId());
	}
	
	@Test
	public void testGetFullSchema() throws ServletException, IOException, JSONObjectAdapterException{
		ObjectSchema full = entityServletHelper.getFullSchema(Study.class.getName());
		assertNotNull(full);
		// This class should implement entity.
		assertNotNull(full.getImplements());
		assertNotNull(full.getImplements().length > 0);
	}
	
	@Test
	public void testGetRegistry() throws ServletException, IOException, JSONObjectAdapterException{
		EntityRegistry registry = entityServletHelper.getEntityRegistry();
		assertNotNull(registry);
		assertNotNull(registry.getEntityTypes());
		assertTrue(registry.getEntityTypes().size() > 0);
	}
	
	@Test (expected=NameConflictException.class)
	public void testPLFM_449NameConflict() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		p = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		toDelete.add(p.getId());
		
		Study one = new Study();
		one.setName("one");
		one.setParentId(p.getId());
		one.setEntityType(Study.class.getName());
		one = (Study) entityServletHelper.createEntity(one, TEST_USER1);
		// Now try to re-use the name
		Study two = new Study();
		two.setName("one");
		two.setParentId(p.getId());
		two.setEntityType(Study.class.getName());
		two = (Study) entityServletHelper.createEntity(two, TEST_USER1);
	}

	@Test
	public void testPLFM_1288() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		p = (Project) entityServletHelper.createEntity(p, TEST_USER1);
		toDelete.add(p.getId());
		
		Study one = new Study();
		one.setName("one");
		one.setParentId(p.getId());
		one.setEntityType(Study.class.getName());
		one = (Study) entityServletHelper.createEntity(one, TEST_USER1);
		// Now try to re-use the name
		Code two = new Code();
		two.setName("code");
		two.setParentId(one.getId());
		two.setEntityType(Code.class.getName());
		try{
			two = (Code) entityServletHelper.createEntity(two, TEST_USER1);
			fail("Code cannot have a parent of type Study");
		}catch(IllegalArgumentException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf(Code.class.getName()) > 0);
			assertTrue(e.getMessage().indexOf(Study.class.getName()) > 0);
		}
		
	}
	
	@Ignore
	@Test
	public void testChangeEntityType() throws Exception {
		Study s = new Study();
		s.setName("StudyToChangeToFolder");
		s.setEntityType(s.getClass().getName());
		s = (Study) entityServletHelper.createEntity(s, TEST_USER1);
		assertNotNull(s);
		String studyId = s.getId();
		// Get the annotaions for this entity
		Annotations annots = entityServletHelper.getEntityAnnotaions(studyId, TEST_USER1);
		assertNotNull(annots);
		annots.addAnnotation("doubleAnno", new Double(45.0001));
		annots.addAnnotation("string", "A string");
		Annotations annosClone = entityServletHelper.updateAnnotations(annots, TEST_USER1);
		assertNotNull(annosClone);
		assertEquals(studyId, annosClone.getId());
		assertFalse(annots.getEtag().equals(annosClone.getEtag()));

		TypeChangeRequest req = new TypeChangeRequest();
		req.setNewType("folder");
		entityServletHelper.changeEntityType(s.getId(), s.getEtag(), TEST_USER1, req);
		Folder f = (Folder)entityServletHelper.getEntity(s.getId(), TEST_USER1);
		assertNotNull(f);
		assertEquals(studyId, f.getId());
		annots = entityServletHelper.getEntityAnnotaions(studyId, TEST_USER1);
		assertNotNull(annots);
		assertEquals(studyId, annots.getId());
		String value = (String) annots.getSingleValue("string");
		assertEquals("A string", value);
		assertEquals(new Double(45.0001), annots.getSingleValue("doubleAnno"));
		
	}
	
}
