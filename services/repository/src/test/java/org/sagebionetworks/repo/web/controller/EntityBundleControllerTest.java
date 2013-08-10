package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" }, loader =MockWebApplicationContextLoader.class)
@MockWebApplication
public class EntityBundleControllerTest {
	
	private static final String DUMMY_STUDY_2 = "Test Study 2";
	private static final String DUMMY_STUDY_1 = "Test Study 1";
	private static final String DUMMY_PROJECT = "Test Project";

	@Autowired
	private EntityServletTestHelper entityServletHelper;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private UserManager userManager;
	@Autowired
	private NodeManager nodeManager;

	private static final String TEST_USER1 = TestUserDAO.TEST_USER_NAME;
	
	private List<String> toDelete = null;
	S3FileHandle handleOne;
	S3FileHandle handleTwo;
	private String userName;
	private String ownerId;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityServletHelper);
		assertNotNull(fileMetadataDao);
		assertNotNull(userManager);
		assertNotNull(nodeManager);
		toDelete = new ArrayList<String>();
		userName = TestUserDAO.TEST_USER_NAME;
		ownerId = userManager.getUserInfo(userName).getIndividualGroup().getId();
	}
	
	@After
	public void after() throws Exception {
		if(toDelete != null){
			UserInfo testUserInfo = userManager.getUserInfo(TEST_USER1);
			for(String id: toDelete){
				try {
					nodeManager.delete(testUserInfo, id);
				} catch (Exception e) {
					// Try even if it fails.
				}
			}
		}
		if(handleOne != null && fileMetadataDao != null){
			try{
				fileMetadataDao.delete(handleOne.getId());
			}catch(Exception e){}
		}
		if(handleTwo != null && fileMetadataDao != null){
			try{
				fileMetadataDao.delete(handleTwo.getId());
			}catch(Exception e){}
		}
	}
	
	
	@Test
	public void testGetEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Study s1 = new Study();
		s1.setName(DUMMY_STUDY_1);
		s1.setEntityType(s1.getClass().getName());
		s1.setParentId(id);
		s1 = (Study) entityServletHelper.createEntity(s1, TEST_USER1, null);
		toDelete.add(s1.getId());
		
		Study s2 = new Study();
		s2.setName(DUMMY_STUDY_2);
		s2.setEntityType(s2.getClass().getName());
		s2.setParentId(id);
		s2 = (Study) entityServletHelper.createEntity(s2, TEST_USER1, null);
		toDelete.add(s2.getId());
		
		// Get/add/update annotations for this entity
		Annotations a = entityServletHelper.getEntityAnnotaions(id, TEST_USER1);
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");
		Annotations a2 = entityServletHelper.updateAnnotations(a, TEST_USER1);
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ANNOTATIONS |
					EntityBundle.PERMISSIONS |
					EntityBundle.ENTITY_PATH |
					EntityBundle.ENTITY_REFERENCEDBY |
					EntityBundle.HAS_CHILDREN |
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(id, mask, TEST_USER1);
		Project p3 = (Project) eb.getEntity();
		assertFalse("Etag should have been updated, but was not", p3.getEtag().equals(p2.getEtag()));
		p2.setEtag(p3.getEtag());
		assertEquals(p2, p3);
		
		Annotations a3 = eb.getAnnotations();
		assertFalse("Etag should have been updated, but was not", a3.getEtag().equals(a.getEtag()));
		assertEquals("Retrieved Annotations in bundle do not match original ones", a2, a3);
		
		UserEntityPermissions uep = eb.getPermissions();
		assertNotNull("Permissions were requested, but null in bundle", uep);
		assertTrue("Invalid Permissions", uep.getCanEdit());
		
		EntityPath path = eb.getPath();
		assertNotNull("Path was requested, but null in bundle", path);
		assertNotNull("Invalid path", path.getPath());
		
		List<EntityHeader> rb = eb.getReferencedBy();
		assertNotNull("ReferencedBy was requested, but null in bundle", rb);
		
		Boolean hasChildren = eb.getHasChildren();
		assertNotNull("HasChildren was requested, but null in bundle", hasChildren);
		assertEquals("HasChildren incorrect", Boolean.TRUE, hasChildren);
		
		AccessControlList acl = eb.getAccessControlList();
		assertNotNull("AccessControlList was requested, but null in bundle", acl);
	}
	
	@Test
	public void testGetEntityBundleInheritedACL() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Study s1 = new Study();
		s1.setName(DUMMY_STUDY_1);
		s1.setEntityType(s1.getClass().getName());
		s1.setParentId(id);
		s1 = (Study) entityServletHelper.createEntity(s1, TEST_USER1, null);
		toDelete.add(s1.getId());
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(s1.getId(), mask, TEST_USER1);
		Study s2 = (Study) eb.getEntity();
		assertTrue("Etags do not match.", s2.getEtag().equals(s1.getEtag()));
		assertEquals(s1, s2);
		
		AccessControlList acl = eb.getAccessControlList();
		assertNull("AccessControlList is inherited; should have been null in bundle.", acl);
	}
		
	/**
	 * Test that proper versions are returned
	 * @throws NameConflictException
	 * @throws JSONObjectAdapterException
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@Test
	public void testGetEntityBundleForVersion() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {		
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		String parentId = p2.getId();
		toDelete.add(parentId);
		
		Data d1 = new Data();
		d1.setName("Dummy Data 1");
		d1.setParentId(parentId);
		d1.setEntityType(d1.getClass().getName());
		LocationData d1Location = new LocationData();
		d1Location.setPath("fakepath");
		d1Location.setType(LocationTypeNames.external);		
		d1.setLocations(Arrays.asList(new LocationData[] { d1Location }));
		d1.setMd5("c88c3db97754be31f9242eb3c08382ee");
		d1 = (Data) entityServletHelper.createEntity(d1, TEST_USER1, null);
		toDelete.add(d1.getId());
		
		// Get/add/update annotations for this entity
		Annotations a1 = entityServletHelper.getEntityAnnotaions(d1.getId(), TEST_USER1);
		a1.addAnnotation("v1", new Long(1));
		a1 = entityServletHelper.updateAnnotations(a1, TEST_USER1);
		a1 = entityServletHelper.getEntityAnnotaions(d1.getId(), TEST_USER1);
	
		// create 2nd version of entity and annotations
		d1 = (Data) entityServletHelper.getEntity(d1.getId(), TEST_USER1);
		d1Location = new LocationData();
		d1Location.setPath("fakepath_2");
		d1Location.setType(LocationTypeNames.external);		
		d1.setLocations(Arrays.asList(new LocationData[] { d1Location }));
		d1.setMd5("c88c3db97754be31f9242eb3c08382e0");
		entityServletHelper.updateEntity(d1, TEST_USER1);
		// Get/add/update annotations for this entity
		Annotations a2 = entityServletHelper.getEntityAnnotaions(d1.getId(), TEST_USER1);
		a2.addAnnotation("v2", new Long(2));
		a2 = entityServletHelper.updateAnnotations(a2, TEST_USER1);
		a2 = entityServletHelper.getEntityAnnotaions(d1.getId(), TEST_USER1);
		
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ANNOTATIONS |
					EntityBundle.ENTITY_REFERENCEDBY;
		// Get the bundle for version 1, verify contents
		Long versionNumber = new Long(1);
		EntityBundle eb = entityServletHelper.getEntityBundleForVersion(d1.getId(), versionNumber, mask, TEST_USER1);
		Data d2 = (Data) eb.getEntity();
		assertEquals(versionNumber, d2.getVersionNumber());
		
		Annotations a3 = eb.getAnnotations();
		assertTrue(a3.getLongAnnotations().containsKey("v1"));
		assertFalse(a3.getLongAnnotations().containsKey("v2"));
		
		// Get the bundle for version 2, verify contents
		versionNumber = new Long(2);
		EntityBundle eb2 = entityServletHelper.getEntityBundleForVersion(d1.getId(), versionNumber, mask, TEST_USER1);
		d2 = (Data) eb2.getEntity();
		assertEquals(versionNumber, d2.getVersionNumber());
		
		a3 = eb2.getAnnotations();
		assertTrue(a3.getLongAnnotations().containsKey("v1"));
		assertTrue(a3.getLongAnnotations().containsKey("v2"));	
	}
	
	@Test
	public void testGetPartialEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		String id = p2.getId();
		toDelete.add(id);
		
		// Get/add/update annotations for this entity
		Annotations a = entityServletHelper.getEntityAnnotaions(id, TEST_USER1);
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");
		entityServletHelper.updateAnnotations(a, TEST_USER1);
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY;
		EntityBundle eb = entityServletHelper.getEntityBundle(id, mask, TEST_USER1);
		Project p3 = (Project) eb.getEntity();
		assertFalse("Etag should have been updated, but was not", p3.getEtag().equals(p2.getEtag()));
		p2.setEtag(p3.getEtag());
		assertEquals(p2, p3);
		
		Annotations a3 = eb.getAnnotations();
		assertNull("Annotations were not requested, but were returned in bundle", a3);
		
		UserEntityPermissions uep = eb.getPermissions();
		assertNull("Permissions were not requested, but were returned in bundle", uep);
		
		EntityPath path = eb.getPath();
		assertNull("Path was not requested, but were returned in bundle", path);
		
		List<EntityHeader> rb = eb.getReferencedBy();
		assertNull("ReferencedBy was not requested, but were returned in bundle", rb);
		
		Boolean hasChildren = eb.getHasChildren();
		assertNull("HasChildren was not requested, but were returned in bundle", hasChildren);
		
		AccessControlList acl = eb.getAccessControlList();
		assertNull("AccessControlList was not requested, but were returned in bundle", acl);
	}
	
	@Test
	public void testGetFileHandle() throws NameConflictException, DatastoreException, JSONObjectAdapterException, ServletException, IOException, NotFoundException{
		
		S3FileHandle handle = new S3FileHandle();
		// Create a file handle
		handle = new S3FileHandle();
		handle.setCreatedBy(ownerId);
		handle.setCreatedOn(new Date());
		handle.setBucketName("bucket");
		handle.setKey("EntityControllerTest.testGetFileHandle1");
		handle.setEtag("etag");
		handle.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handle);
		// Second handle
		handle.setKey("EntityControllerTest.testGetFileHandle2");
		handle.setFileName("fo2o.bar");
		handleTwo = fileMetadataDao.createFile(handle);
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		String id = p2.getId();
		toDelete.add(id);
		
		FileEntity file = new FileEntity();
		file.setParentId(p.getId());
		file.setDataFileHandleId(handleOne.getId());
		file.setEntityType(FileEntity.class.getName());
		file = (FileEntity) entityServletHelper.createEntity(file, TEST_USER1, null);
		// Get the file handle in the bundle
		EntityBundle bundle = entityServletHelper.getEntityBundle(file.getId(), EntityBundle.FILE_HANDLES, TEST_USER1);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleOne.getId(), bundle.getFileHandles().get(0).getId());
		// Same test with a verion number
		// Update the file 
		file.setDataFileHandleId(handleTwo.getId());
		file = (FileEntity) entityServletHelper.updateEntity(file, TEST_USER1);
		assertEquals("Changing the fileHandle should have created a new version", new Long(2), file.getVersionNumber());
		// Get version one.
		bundle = entityServletHelper.getEntityBundleForVersion(file.getId(), new Long(1), EntityBundle.FILE_HANDLES, TEST_USER1);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleOne.getId(), bundle.getFileHandles().get(0).getId());
		// Get version two
		bundle = entityServletHelper.getEntityBundleForVersion(file.getId(), new Long(2), EntityBundle.FILE_HANDLES, TEST_USER1);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleTwo.getId(), bundle.getFileHandles().get(0).getId());
	}

}
