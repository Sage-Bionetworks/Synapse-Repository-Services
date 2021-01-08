package org.sagebionetworks.repo.web.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityBundleControllerTest extends AbstractAutowiredControllerJunit5TestBase {
	
	private static final String DUMMY_STUDY_2 = "Test Study 2";
	private static final String DUMMY_STUDY_1 = "Test Study 1";
	private static final String DUMMY_PROJECT = "Test Project";

	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private IdGenerator idGenerator;

	private List<String> filesToDelete;
	private List<String> toDelete;
	
	private Long adminUserId;
	private String adminUserIdString;
	private UserInfo adminUserInfo;

	@BeforeEach
	public void setUp() throws Exception {
		assertNotNull(fileHandleDao);
		assertNotNull(userManager);
		assertNotNull(nodeManager);
		toDelete = new ArrayList<String>();
		filesToDelete = new ArrayList<String>();
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		adminUserInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
	}
	
	@AfterEach
	public void after() throws Exception {
		for (String id : toDelete) {
			try {
				nodeManager.delete(adminUserInfo, id);
			} catch (NotFoundException e) { }
		}
		
		for (String id : filesToDelete) {
			fileHandleDao.delete(id);
		}
	}
	
	
	@Test
	public void testGetEntityBundle() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Folder s1 = new Folder();
		s1.setName(DUMMY_STUDY_1);
		s1.setParentId(id);
		s1 = (Folder) entityServletHelper.createEntity(s1, adminUserId, null);
		toDelete.add(s1.getId());
		
		Folder s2 = new Folder();
		s2.setName(DUMMY_STUDY_2);
		s2.setParentId(id);
		s2 = (Folder) entityServletHelper.createEntity(s2, adminUserId, null);
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
					EntityBundle.HAS_CHILDREN |
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(id, mask, adminUserId);
		Project p3 = (Project) eb.getEntity();
		assertFalse(p3.getEtag().equals(p2.getEtag()), "Etag should have been updated, but was not");
		p2.setEtag(p3.getEtag());
		p2.setModifiedOn(p3.getModifiedOn());
		assertEquals(p2, p3);
		
		Annotations a3 = eb.getAnnotations();
		assertFalse(a3.getEtag().equals(a.getEtag()), "Etag should have been updated, but was not");
		assertEquals(a2, a3, "Retrieved Annotations in bundle do not match original ones");
		
		UserEntityPermissions uep = eb.getPermissions();
		assertNotNull(uep, "Permissions were requested, but null in bundle");
		assertTrue(uep.getCanEdit(), "Invalid Permissions");
		
		EntityPath path = eb.getPath();
		assertNotNull(path, "Path was requested, but null in bundle");
		assertNotNull(path.getPath(), "Invalid path");
		
		Boolean hasChildren = eb.getHasChildren();
		assertNotNull(hasChildren, "HasChildren was requested, but null in bundle");
		assertEquals(Boolean.TRUE, hasChildren, "HasChildren incorrect");
		
		AccessControlList acl = eb.getAccessControlList();
		assertNotNull(acl, "AccessControlList was requested, but null in bundle");
	}
	
	@Test
	public void testGetEntityBundleInheritedACL() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		Folder s1 = new Folder();
		s1.setName(DUMMY_STUDY_1);
		s1.setParentId(id);
		s1 = (Folder) entityServletHelper.createEntity(s1, adminUserId, null);
		toDelete.add(s1.getId());
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY | 
					EntityBundle.ACL;
		EntityBundle eb = entityServletHelper.getEntityBundle(s1.getId(), mask, adminUserId);
		Folder s2 = (Folder) eb.getEntity();
		assertTrue(s2.getEtag().equals(s1.getEtag()), "Etags do not match.");
		assertEquals(s1, s2);
		
		AccessControlList acl = eb.getAccessControlList();
		assertNull(acl, "AccessControlList is inherited; should have been null in bundle.");
	}
	
	@Test
	public void testGetPartialEntityBundle() throws Exception {
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
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
		assertFalse(p3.getEtag().equals(p2.getEtag()), "Etag should have been updated, but was not");
		p2.setEtag(p3.getEtag());
		p2.setModifiedOn(p3.getModifiedOn());
		assertEquals(p2, p3);
		
		Annotations a3 = eb.getAnnotations();
		assertNull(a3, "Annotations were not requested, but were returned in bundle");
		
		UserEntityPermissions uep = eb.getPermissions();
		assertNull(uep, "Permissions were not requested, but were returned in bundle");
		
		EntityPath path = eb.getPath();
		assertNull(path, "Path was not requested, but were returned in bundle");
		
		List<EntityHeader> rb = eb.getReferencedBy();
		assertNull(rb, "ReferencedBy was not requested, but were returned in bundle");
		
		Boolean hasChildren = eb.getHasChildren();
		assertNull(hasChildren, "HasChildren was not requested, but were returned in bundle");
		
		AccessControlList acl = eb.getAccessControlList();
		assertNull(acl, "AccessControlList was not requested, but were returned in bundle");
	}
	
	@Test
	public void testGetFileHandle() throws Exception{
		
		S3FileHandle handleOne = TestUtils.createS3FileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setKey("EntityControllerTest.testGetFileHandle1");
		handleOne.setContentMd5("md51");
		
		S3FileHandle handleTwo = TestUtils.createS3FileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleTwo.setKey("EntityControllerTest.testGetFileHandle2");
		handleTwo.setContentMd5("md52");

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(handleOne);
		fileHandleToCreate.add(handleTwo);
		fileHandleDao.createBatch(fileHandleToCreate);
		
		handleOne = (S3FileHandle) fileHandleDao.get(handleOne.getId());
		filesToDelete.add(handleOne.getId());
		handleTwo = (S3FileHandle) fileHandleDao.get(handleTwo.getId());
		filesToDelete.add(handleTwo.getId());
		
		// Create an entity
		Project p = new Project();
		p.setName(DUMMY_PROJECT);
		Project p2 = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = p2.getId();
		toDelete.add(id);
		
		FileEntity file = new FileEntity();
		file.setParentId(p2.getId());
		file.setDataFileHandleId(handleOne.getId());
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
		assertEquals(new Long(2), file.getVersionNumber(), "Changing the fileHandle should have created a new version");
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
