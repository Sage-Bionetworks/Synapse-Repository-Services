package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class EntityBundleControllerTest extends AbstractAutowiredControllerTestBase {
	
	private static final String DUMMY_STUDY_2 = "Test Study 2";
	private static final String DUMMY_STUDY_1 = "Test Study 1";
	private static final String DUMMY_PROJECT = "Test Project";

	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;

	private List<String> filesToDelete;
	private List<String> toDelete;
	
	private Long adminUserId;
	private String adminUserIdString;
	private UserInfo adminUserInfo;

	@Before
	public void setUp() throws Exception {
		assertNotNull(fileMetadataDao);
		assertNotNull(userManager);
		assertNotNull(nodeManager);
		toDelete = new ArrayList<String>();
		filesToDelete = new ArrayList<String>();
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		adminUserInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}
	
	@After
	public void after() throws Exception {
		for (String id : toDelete) {
			try {
				nodeManager.delete(adminUserInfo, id);
			} catch (NotFoundException e) { }
		}
		
		for (String id : filesToDelete) {
			fileMetadataDao.delete(id);
		}
	}
	
	
	@Test
	public void testGetEntityBundle() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Study s1 = new Study();
		s1.setName(DUMMY_STUDY_1);
		s1.setEntityType(s1.getClass().getName());
		s1.setParentId(id);
		s1 = (Study) entityServletHelper.createEntity(s1, adminUserId, null);
		toDelete.add(s1.getId());
		
		Study s2 = new Study();
		s2.setName(DUMMY_STUDY_2);
		s2.setEntityType(s2.getClass().getName());
		s2.setParentId(id);
		s2 = (Study) entityServletHelper.createEntity(s2, adminUserId, null);
		toDelete.add(s2.getId());
		
		// Get/add/update annotations for this entity
		Annotations a = entityServletHelper.getEntityAnnotations(id, adminUserId);
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");
		Annotations a2 = entityServletHelper.updateAnnotations(a, adminUserId);
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ANNOTATIONS |
					EntityBundle.PERMISSIONS |
					EntityBundle.ENTITY_PATH |
					EntityBundle.ENTITY_REFERENCEDBY |
					EntityBundle.HAS_CHILDREN |
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(id, mask, adminUserId);
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
	public void testGetEntityBundleInheritedACL() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Study s1 = new Study();
		s1.setName(DUMMY_STUDY_1);
		s1.setEntityType(s1.getClass().getName());
		s1.setParentId(id);
		s1 = (Study) entityServletHelper.createEntity(s1, adminUserId, null);
		toDelete.add(s1.getId());
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(s1.getId(), mask, adminUserId);
		Study s2 = (Study) eb.getEntity();
		assertTrue("Etags do not match.", s2.getEtag().equals(s1.getEtag()));
		assertEquals(s1, s2);
		
		AccessControlList acl = eb.getAccessControlList();
		assertNull("AccessControlList is inherited; should have been null in bundle.", acl);
	}
		
	/**
	 * Test that proper versions are returned
	 */
	@Test
	public void testGetEntityBundleForVersion() throws Exception {		
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
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
		d1 = (Data) entityServletHelper.createEntity(d1, adminUserId, null);
		toDelete.add(d1.getId());
		
		// Get/add/update annotations for this entity
		Annotations a1 = entityServletHelper.getEntityAnnotations(d1.getId(), adminUserId);
		a1.addAnnotation("v1", new Long(1));
		a1 = entityServletHelper.updateAnnotations(a1, adminUserId);
		a1 = entityServletHelper.getEntityAnnotations(d1.getId(), adminUserId);
	
		// create 2nd version of entity and annotations
		d1 = (Data) entityServletHelper.getEntity(d1.getId(), adminUserId);
		d1Location = new LocationData();
		d1Location.setPath("fakepath_2");
		d1Location.setType(LocationTypeNames.external);		
		d1.setLocations(Arrays.asList(new LocationData[] { d1Location }));
		d1.setMd5("c88c3db97754be31f9242eb3c08382e0");
		entityServletHelper.updateEntity(d1, adminUserId);
		// Get/add/update annotations for this entity
		Annotations a2 = entityServletHelper.getEntityAnnotations(d1.getId(), adminUserId);
		a2.addAnnotation("v2", new Long(2));
		a2 = entityServletHelper.updateAnnotations(a2, adminUserId);
		a2 = entityServletHelper.getEntityAnnotations(d1.getId(), adminUserId);
		
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ANNOTATIONS |
					EntityBundle.ENTITY_REFERENCEDBY;
		// Get the bundle for version 1, verify contents
		Long versionNumber = new Long(1);
		EntityBundle eb = entityServletHelper.getEntityBundleForVersion(d1.getId(), versionNumber, mask, adminUserId);
		Data d2 = (Data) eb.getEntity();
		assertEquals(versionNumber, d2.getVersionNumber());
		
		Annotations a3 = eb.getAnnotations();
		assertTrue(a3.getLongAnnotations().containsKey("v1"));
		assertFalse(a3.getLongAnnotations().containsKey("v2"));
		
		// Get the bundle for version 2, verify contents
		versionNumber = new Long(2);
		EntityBundle eb2 = entityServletHelper.getEntityBundleForVersion(d1.getId(), versionNumber, mask, adminUserId);
		d2 = (Data) eb2.getEntity();
		assertEquals(versionNumber, d2.getVersionNumber());
		
		a3 = eb2.getAnnotations();
		assertTrue(a3.getLongAnnotations().containsKey("v1"));
		assertTrue(a3.getLongAnnotations().containsKey("v2"));	
	}
	
	@Test
	public void testGetPartialEntityBundle() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		// Get/add/update annotations for this entity
		Annotations a = entityServletHelper.getEntityAnnotations(id, adminUserId);
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");
		entityServletHelper.updateAnnotations(a, adminUserId);
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY;
		EntityBundle eb = entityServletHelper.getEntityBundle(id, mask, adminUserId);
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
	public void testGetFileHandle() throws Exception{
		
		S3FileHandle handle = new S3FileHandle();
		// Create a file handle
		handle = new S3FileHandle();
		handle.setCreatedBy(adminUserIdString);
		handle.setCreatedOn(new Date());
		handle.setBucketName("bucket");
		handle.setKey("EntityControllerTest.testGetFileHandle1");
		handle.setEtag("etag");
		handle.setFileName("foo.bar");
		S3FileHandle handleOne = fileMetadataDao.createFile(handle);
		filesToDelete.add(handleOne.getId());
		
		// Second handle
		handle.setKey("EntityControllerTest.testGetFileHandle2");
		handle.setFileName("fo2o.bar");
		S3FileHandle handleTwo = fileMetadataDao.createFile(handle);
		filesToDelete.add(handleTwo.getId());
		
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		p.setEntityType(p.getClass().getName());
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		FileEntity file = new FileEntity();
		file.setParentId(p.getId());
		file.setDataFileHandleId(handleOne.getId());
		file.setEntityType(FileEntity.class.getName());
		file = (FileEntity) entityServletHelper.createEntity(file, adminUserId, null);
		toDelete.add(file.getId());
		
		// Get the file handle in the bundle
		EntityBundle bundle = entityServletHelper.getEntityBundle(file.getId(), EntityBundle.FILE_HANDLES, adminUserId);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleOne.getId(), bundle.getFileHandles().get(0).getId());
		// Same test with a verion number
		// Update the file 
		file.setDataFileHandleId(handleTwo.getId());
		file = (FileEntity) entityServletHelper.updateEntity(file, adminUserId);
		assertEquals("Changing the fileHandle should have created a new version", new Long(2), file.getVersionNumber());
		// Get version one.
		bundle = entityServletHelper.getEntityBundleForVersion(file.getId(), new Long(1), EntityBundle.FILE_HANDLES, adminUserId);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleOne.getId(), bundle.getFileHandles().get(0).getId());
		// Get version two
		bundle = entityServletHelper.getEntityBundleForVersion(file.getId(), new Long(2), EntityBundle.FILE_HANDLES, adminUserId);
		assertNotNull(bundle);
		assertNotNull(bundle.getFileHandles());
		assertTrue(bundle.getFileHandles().size() > 0);
		assertNotNull(bundle.getFileHandles().get(0));
		assertEquals(handleTwo.getId(), bundle.getFileHandles().get(0).getId());
	}

}
