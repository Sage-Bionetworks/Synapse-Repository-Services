package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityControllerTest extends AbstractAutowiredControllerTestBase {

	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private IdGenerator idGenerator;
	
	private List<String> toDelete;
	private S3FileHandle handleOne;
	private PreviewFileHandle previewOne;
	private S3FileHandle handleTwo;
	private PreviewFileHandle previewTwo;
	
	private Long adminUserId;
	private String adminUserIdString;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(fileHandleDao);
		assertNotNull(userManager);
		assertNotNull(nodeManager);
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		
		toDelete = new ArrayList<String>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserIdString);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setEtag(UUID.randomUUID().toString());
		// Create a preview
		previewOne = new PreviewFileHandle();
		previewOne.setCreatedBy(adminUserIdString);
		previewOne.setCreatedOn(new Date());
		previewOne.setBucketName("bucket");
		previewOne.setKey("EntityControllerTest.previewFileKey");
		previewOne.setEtag("etag");
		previewOne.setFileName("bar.txt");
		previewOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewOne.setEtag(UUID.randomUUID().toString());
		
		// Create a file handle
		handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(adminUserIdString);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleTwo.setEtag(UUID.randomUUID().toString());
		// Create a preview
		previewTwo = new PreviewFileHandle();
		previewTwo.setCreatedBy(adminUserIdString);
		previewTwo.setCreatedOn(new Date());
		previewTwo.setBucketName("bucket");
		previewTwo.setKey("EntityControllerTest.previewFileKeyTwo");
		previewTwo.setEtag("etag");
		previewTwo.setFileName("bar2.txt");
		previewTwo.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewTwo.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(handleOne);
		fileHandleToCreate.add(handleTwo);
		fileHandleToCreate.add(previewOne);
		fileHandleToCreate.add(previewTwo);
		fileHandleDao.createBatch(fileHandleToCreate);

		handleOne = (S3FileHandle) fileHandleDao.get(handleOne.getId());
		previewOne = (PreviewFileHandle) fileHandleDao.get(previewOne.getId());
		handleTwo = (S3FileHandle) fileHandleDao.get(handleTwo.getId());
		previewTwo = (PreviewFileHandle) fileHandleDao.get(previewTwo.getId());
		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleOne.getId(), previewOne.getId());

		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleTwo.getId(), previewTwo.getId());
	}
	
	@After
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(adminUserId);
		for(String id: toDelete){
			try {
				nodeManager.delete(adminUserInfo, id);
			} catch (Exception e) {
				// Try even if it fails.
			}
		}
		fileHandleDao.delete(handleOne.getId());
		fileHandleDao.delete(previewOne.getId());
		fileHandleDao.delete(handleTwo.getId());
		fileHandleDao.delete(previewTwo.getId());
	}
	
	@Test
	public void testCRUDEntity() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());		
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		assertEquals(p.getName(), clone.getName());
		// Now get the entity with the ID
		Project clone2 = (Project) entityServletHelper.getEntity(id, adminUserId);
		assertEquals(clone, clone2);
		// Make sure we can update it
		clone2.setName("My new name");
		Project clone3 = (Project) entityServletHelper.updateEntity(clone2, adminUserId);
		assertNotNull(clone3);		
		assertEquals(clone2.getName(), clone3.getName());
		// Should not match the original
		assertFalse(p.getName().equals(clone3.getName()));
		// the Etag should have changed
		assertFalse(clone2.getEtag().equals(clone3.getEtag()));
		// Now delete it
		entityServletHelper.deleteEntity(id, adminUserId);
		// it should not be found now
		try{
			entityServletHelper.getEntity(id, adminUserId);
			fail("Delete failed");
		}catch (NotFoundException e) {
			// expected
		}
	}
	
	@Test
	public void testAnnotationsCRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		Annotations annos = entityServletHelper.getEntityAnnotations(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		// Updte them
		Annotations annosClone = entityServletHelper.updateAnnotations(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = (String) annosClone.getSingleValue("string");
		assertEquals("A string", value);
		assertEquals(new Double(45.0001), annosClone.getSingleValue("doubleAnno"));
		
	}
	
	@Test
	public void testNaNAnnotationsCRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		Annotations annos = entityServletHelper.getEntityAnnotations(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		annos.addAnnotation("doubleAnno", new Double(Double.NaN));
		annos.addAnnotation("string", "A string");
		// Update them
		Annotations annosClone = entityServletHelper.updateAnnotations(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = (String) annosClone.getSingleValue("string");
		assertEquals("A string", value);
		assertEquals(new Double(Double.NaN), annosClone.getSingleValue("doubleAnno"));
		
	}
	
	@Test
	public void testGetUserEntityPermissions() throws Exception{
		Project p = new Project();
		p.setName("UserEntityPermissions");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		UserEntityPermissions uep = entityServletHelper.getUserEntityPermissions(id, adminUserId);
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
			Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
			String id = clone.getId();
			toDelete.add(id);
			ids.add(id);
		}
	
		PaginatedResults<EntityHeader> results = entityServletHelper.getEntityTypeBatch(ids, adminUserId);
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
	public void testEntityPath() throws Exception{
		Project p = new Project();
		p.setName("EntityPath");
		p.setEntityType(p.getClass().getName());
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		EntityPath path = entityServletHelper.getEntityPath(id, adminUserId);
		assertNotNull(path);
		assertNotNull(path.getPath());
		assertEquals(2, path.getPath().size());
		EntityHeader header = path.getPath().get(1);
		assertNotNull(header);
		assertEquals(id, header.getId());
	}
	
	@Test
	public void testGetRESTResources() throws Exception {
		RestResourceList rrl = entityServletHelper.getRESTResources();
		assertNotNull(rrl);
		assertNotNull(rrl.getList());
		assertTrue(rrl.getList().size() > 0);
	}
	
	
	@Test
	public void testGetEffectiveSchema() throws Exception {
		String resourceId = FileEntity.class.getName();
		ObjectSchema effective = entityServletHelper.getEffectiveSchema(FileEntity.class.getName());
		assertNotNull(effective);
		assertEquals(resourceId, effective.getId());
	}
	
	@Test
	public void testGetFullSchema() throws Exception {
		ObjectSchema full = entityServletHelper.getFullSchema(FileEntity.class.getName());
		assertNotNull(full);
		// This class should implement entity.
		assertNotNull(full.getImplements());
		assertNotNull(full.getImplements().length > 0);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetRegistry() throws Exception {
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
		p = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		toDelete.add(p.getId());
		
		Folder one = new Folder();
		one.setName("one");
		one.setParentId(p.getId());
		one.setEntityType(Folder.class.getName());
		one = (Folder) entityServletHelper.createEntity(one, adminUserId, null);
		// Now try to re-use the name
		Folder two = new Folder();
		two.setName("one");
		two.setParentId(p.getId());
		two.setEntityType(Folder.class.getName());
		two = (Folder) entityServletHelper.createEntity(two, adminUserId, null);
	}

	@Test(expected=NotFoundException.class)
	public void testActivityId404() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		String activityId = "123456789";
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, activityId);
		String id = clone.getId();
		toDelete.add(id);
	}

	/**
	 * Test that we can create a file entity.
	 * @throws NotFoundException 
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 * @throws Exception 
	 */
	@Test
	public void testFileEntityCRUD() throws Exception {
		// Create a project
		Project parent = new Project();
		parent.setName("project");
		parent.setEntityType(Project.class.getName());
		Project parentClone = (Project) entityServletHelper.createEntity(parent, adminUserId, null);
		String parentId = parentClone.getId();
		toDelete.add(parentId);
		// Create a file entity
		FileEntity file = new FileEntity();
		file.setName("FileName");
		file.setEntityType(FileEntity.class.getName());
		file.setParentId(parentId);
		file.setDataFileHandleId(handleOne.getId());
		// Save it
		file = (FileEntity) entityServletHelper.createEntity(file, adminUserId, null);
		assertNotNull(file);
		assertNotNull(file.getId());
		toDelete.add(file.getId());
		assertEquals(handleOne.getId(), file.getDataFileHandleId());
		// Sshould start on V1
		assertEquals(new Long(1), file.getVersionNumber());
		
		// Now create a second version using the second files
		file.setDataFileHandleId(handleTwo.getId());
		file.setVersionComment("V2 comment");
		file.setVersionLabel("V2");
		file = (FileEntity) entityServletHelper.createNewVersion(adminUserId, file);
		// Validate we are on V3
		assertEquals(new Long(2), file.getVersionNumber());
		// First get the URL for the current version
		URL url = entityServletHelper.getEntityFileURLForCurrentVersion(adminUserId, file.getId(), null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(handleTwo.getKey()) > 0);
		URL urlNoRedirect = entityServletHelper.getEntityFileURLForCurrentVersion(adminUserId, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(handleTwo.getKey()) > 0);
		// Now the first version
		url = entityServletHelper.getEntityFileURLForVersion(adminUserId, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(handleOne.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFileURLForVersion(adminUserId, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(handleOne.getKey()) > 0);
		// Get the preview of the current version
		url = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(adminUserId, file.getId(), null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(previewTwo.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(adminUserId, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(previewTwo.getKey()) > 0);
		// Get the preview of the first version
		url = entityServletHelper.getEntityFilePreviewURLForVersion(adminUserId, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(previewOne.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForVersion(adminUserId, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(previewOne.getKey()) > 0);
		
		// Validate that we can get the files handles
		FileHandleResults fhr = entityServletHelper.geEntityFileHandlesForCurrentVersion(adminUserId, file.getId());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(handleTwo.getId(), fhr.getList().get(0).getId());
		assertEquals(previewTwo.getId(), fhr.getList().get(1).getId());
		// Get the previous version as well
		fhr = entityServletHelper.geEntityFileHandlesForVersion(adminUserId, file.getId(), 1l);
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(handleOne.getId(), fhr.getList().get(0).getId());
		assertEquals(previewOne.getId(), fhr.getList().get(1).getId());
	}


	@Test
	public void testGetEntityHeaderByMd5() throws Exception {

		PaginatedResults<EntityHeader> results = entityServletHelper.getEntityHeaderByMd5(
				adminUserId, "548c050497fb361742b85e0712b0cc96");
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		Project parent = new Project();
		parent.setName("testGetEntityHeaderByMd5");
		parent.setEntityType(Project.class.getName());
		parent = (Project) entityServletHelper.createEntity(parent, adminUserId, null);
		assertNotNull(parent);
		String parentId = parent.getId();
		toDelete.add(parentId);

		FileEntity file = new FileEntity();
		file.setName("testGetEntityHeaderByMd5 file");
		file.setEntityType(FileEntity.class.getName());
		file.setParentId(parentId);
		file.setDataFileHandleId(handleOne.getId());
		file = (FileEntity) entityServletHelper.createEntity(file, adminUserId, null);
		assertNotNull(file);
		assertNotNull(file.getId());
		toDelete.add(file.getId());

		results = entityServletHelper.getEntityHeaderByMd5(adminUserId, handleOne.getContentMd5());
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertEquals(file.getId(), results.getResults().get(0).getId());

		// Move to trash can and we should get back empty results
		entityServletHelper.deleteEntity(file.getId(), adminUserId);
		results = entityServletHelper.getEntityHeaderByMd5(adminUserId, handleOne.getContentMd5());
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
	}
}
