package org.sagebionetworks.repo.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
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
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

public class EntityControllerTest extends AbstractAutowiredControllerJunit5TestBase {

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
	private S3FileHandle previewOne;
	private S3FileHandle handleTwo;
	private S3FileHandle previewTwo;
	
	private Long adminUserId;
	private String adminUserIdString;
	
	private static final String S3_BUCKET_NAME = StackConfigurationSingleton.singleton().getS3Bucket();

	@BeforeEach
	public void setUp() throws Exception {
		assertNotNull(fileHandleDao);
		assertNotNull(userManager);
		assertNotNull(nodeManager);
		
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserIdString = adminUserId.toString();
		
		toDelete = new ArrayList<>();
		// Create a file handle
		handleOne = TestUtils.createS3FileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setBucketName(S3_BUCKET_NAME);
		handleOne.setKey("EntityControllerTest.mainFileKey");
		// Create a preview
		previewOne = TestUtils.createPreviewFileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewOne.setBucketName(S3_BUCKET_NAME);
		previewOne.setKey("EntityControllerTest.previewFileKey");

		// Create a file handle
		handleTwo = TestUtils.createS3FileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleTwo.setBucketName(S3_BUCKET_NAME);
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		// Create a preview
		previewTwo = TestUtils.createPreviewFileHandle(adminUserIdString, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewTwo.setBucketName(S3_BUCKET_NAME);
		previewTwo.setKey("EntityControllerTest.previewFileKeyTwo");

		List<FileHandle> fileHandleToCreate = Arrays.asList(handleOne, handleTwo, previewOne, previewTwo);
		fileHandleDao.createBatch(fileHandleToCreate);

		handleOne = (S3FileHandle) fileHandleDao.get(handleOne.getId());
		previewOne = (S3FileHandle) fileHandleDao.get(previewOne.getId());
		handleTwo = (S3FileHandle) fileHandleDao.get(handleTwo.getId());
		previewTwo = (S3FileHandle) fileHandleDao.get(previewTwo.getId());
		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleOne.getId(), previewOne.getId());

		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleTwo.getId(), previewTwo.getId());
	}
	
	@AfterEach
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
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		org.sagebionetworks.repo.model.Annotations annos = entityServletHelper.getEntityAnnotations(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		// Updte them
		org.sagebionetworks.repo.model.Annotations annosClone = entityServletHelper.updateAnnotations(annos, adminUserId);
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
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		org.sagebionetworks.repo.model.Annotations annos = entityServletHelper.getEntityAnnotations(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		annos.addAnnotation("doubleAnno", new Double(Double.NaN));
		annos.addAnnotation("string", "A string");
		// Update them
		org.sagebionetworks.repo.model.Annotations annosClone = entityServletHelper.updateAnnotations(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = (String) annosClone.getSingleValue("string");
		assertEquals("A string", value);
		assertEquals(new Double(Double.NaN), annosClone.getSingleValue("doubleAnno"));
		
	}


	@Test
	public void testAnnotationsV2CRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		Annotations annos = entityServletHelper.getEntityAnnotationsV2(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		AnnotationsV2TestUtils.putAnnotations(annos,"doubleAnno", "45.0001", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos,"string", "A string", AnnotationsValueType.STRING);
		// Updte them
		Annotations annosClone = entityServletHelper.updateAnnotationsV2(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = AnnotationsV2Utils.getSingleValue(annosClone, "string");
		assertEquals("A string", value);
		assertEquals("45.0001", AnnotationsV2Utils.getSingleValue(annosClone, "doubleAnno"));

	}

	@Test
	public void testNaNAnnotationsV2CRUD() throws Exception {
		Project p = new Project();
		p.setName("AnnotCrud");
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		toDelete.add(id);
		// Get the annotaions for this entity
		Annotations annos = entityServletHelper.getEntityAnnotationsV2(id, adminUserId);
		assertNotNull(annos);
		// Change the values
		AnnotationsV2TestUtils.putAnnotations(annos,"doubleAnno", "nan", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos,"string", "A string", AnnotationsValueType.STRING);
		// Update them
		Annotations annosClone = entityServletHelper.updateAnnotationsV2(annos, adminUserId);
		assertNotNull(annosClone);
		assertEquals(id, annosClone.getId());
		assertFalse(annos.getEtag().equals(annosClone.getEtag()));
		String value = AnnotationsV2Utils.getSingleValue(annosClone, "string");
		assertEquals("A string", value);
		assertEquals("nan", AnnotationsV2Utils.getSingleValue(annosClone, "doubleAnno"));
	}
	
	@Test
	public void testGetUserEntityPermissions() throws Exception{
		Project p = new Project();
		p.setName("UserEntityPermissions");
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
	
	@Test
	public void testPLFM_449NameConflict() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		toDelete.add(p.getId());
		
		Folder one = new Folder();
		one.setName("one");
		one.setParentId(p.getId());
		one = (Folder) entityServletHelper.createEntity(one, adminUserId, null);
		// Now try to re-use the name
		Folder two = new Folder();
		two.setName("one");
		two.setParentId(p.getId());
		assertThrows(NameConflictException.class, ()-> {
			entityServletHelper.createEntity(two, adminUserId, null);
		});
	}

	@Test
	public void testActivityId404() throws Exception{
		Project p = new Project();
		p.setName("My Project");
		String activityId = "123456789";
		assertThrows(NotFoundException.class, ()-> {
			entityServletHelper.createEntity(p, adminUserId, activityId);
		});
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
		Project parentClone = (Project) entityServletHelper.createEntity(parent, adminUserId, null);
		String parentId = parentClone.getId();
		toDelete.add(parentId);
		// Create a file entity
		FileEntity file = new FileEntity();
		file.setName("FileName");
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
		assertTrue(url.toString().indexOf(handleTwo.getKey()) > 0, "Url did not contain the expected key");
		URL urlNoRedirect = entityServletHelper.getEntityFileURLForCurrentVersion(adminUserId, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue(urlNoRedirect.toString().indexOf(handleTwo.getKey()) > 0, "Url did not contain the expected key");
		// Now the first version
		url = entityServletHelper.getEntityFileURLForVersion(adminUserId, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue(url.toString().indexOf(handleOne.getKey()) > 0, "Url did not contain the expected key");
		urlNoRedirect = entityServletHelper.getEntityFileURLForVersion(adminUserId, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue(urlNoRedirect.toString().indexOf(handleOne.getKey()) > 0, "Url did not contain the expected key");
		// Get the preview of the current version
		url = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(adminUserId, file.getId(), null);
		assertNotNull(url);
		assertTrue(url.toString().indexOf(previewTwo.getKey()) > 0, "Url did not contain the expected key");
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(adminUserId, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue(urlNoRedirect.toString().indexOf(previewTwo.getKey()) > 0, "Url did not contain the expected key");
		// Get the preview of the first version
		url = entityServletHelper.getEntityFilePreviewURLForVersion(adminUserId, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue(url.toString().indexOf(previewOne.getKey()) > 0, "Url did not contain the expected key");
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForVersion(adminUserId, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue(urlNoRedirect.toString().indexOf(previewOne.getKey()) > 0, "Url did not contain the expected key");
		
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
		parent = (Project) entityServletHelper.createEntity(parent, adminUserId, null);
		assertNotNull(parent);
		String parentId = parent.getId();
		toDelete.add(parentId);

		FileEntity file = new FileEntity();
		file.setName("testGetEntityHeaderByMd5 file");
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
