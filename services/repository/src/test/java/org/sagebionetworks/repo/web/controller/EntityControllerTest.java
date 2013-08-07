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
import java.util.List;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.Code;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
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
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private UserManager userManager;
	@Autowired
	private NodeManager nodeManager;
	
	private List<String> toDelete = null;
	S3FileHandle handleOne;
	PreviewFileHandle previewOne;
	S3FileHandle handleTwo;
	PreviewFileHandle previewTwo;
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
		userName = TestUserDAO.TEST_USER_NAME;
		ownerId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		toDelete = new ArrayList<String>();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(ownerId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("EntityControllerTest.mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setContentMd5("handleOneContentMd5");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create a preview
		previewOne = new PreviewFileHandle();
		previewOne.setCreatedBy(ownerId);
		previewOne.setCreatedOn(new Date());
		previewOne.setBucketName("bucket");
		previewOne.setKey("EntityControllerTest.previewFileKey");
		previewOne.setEtag("etag");
		previewOne.setFileName("bar.txt");
		previewOne = fileMetadataDao.createFile(previewOne);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), previewOne.getId());
		
		// Create a file handle
		handleTwo = new S3FileHandle();
		handleTwo.setCreatedBy(ownerId);
		handleTwo.setCreatedOn(new Date());
		handleTwo.setBucketName("bucket");
		handleTwo.setKey("EntityControllerTest.mainFileKeyTwo");
		handleTwo.setEtag("etag");
		handleTwo.setFileName("foo2.bar");
		handleTwo = fileMetadataDao.createFile(handleTwo);
		// Create a preview
		previewTwo = new PreviewFileHandle();
		previewTwo.setCreatedBy(ownerId);
		previewTwo.setCreatedOn(new Date());
		previewTwo.setBucketName("bucket");
		previewTwo.setKey("EntityControllerTest.previewFileKeyTwo");
		previewTwo.setEtag("etag");
		previewTwo.setFileName("bar2.txt");
		previewTwo = fileMetadataDao.createFile(previewTwo);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleTwo.getId(), previewTwo.getId());
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
		if(handleOne != null && handleOne.getId() != null){
			fileMetadataDao.delete(handleOne.getId());
		}
		if(previewOne != null && previewOne.getId() != null){
			fileMetadataDao.delete(previewOne.getId());
		}
		if(handleTwo != null && handleTwo.getId() != null){
			fileMetadataDao.delete(handleTwo.getId());
		}
		if(previewTwo != null && previewTwo.getId() != null){
			fileMetadataDao.delete(previewTwo.getId());
		}
	}
	
	@Test
	public void testCRUDEntity() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());		
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
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
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
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
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
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
			Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
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
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
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
		p = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		toDelete.add(p.getId());
		
		Study one = new Study();
		one.setName("one");
		one.setParentId(p.getId());
		one.setEntityType(Study.class.getName());
		one = (Study) entityServletHelper.createEntity(one, TEST_USER1, null);
		// Now try to re-use the name
		Study two = new Study();
		two.setName("one");
		two.setParentId(p.getId());
		two.setEntityType(Study.class.getName());
		two = (Study) entityServletHelper.createEntity(two, TEST_USER1, null);
	}

	@Test
	public void testPLFM_1288() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		p = (Project) entityServletHelper.createEntity(p, TEST_USER1, null);
		toDelete.add(p.getId());
		
		Study one = new Study();
		one.setName("one");
		one.setParentId(p.getId());
		one.setEntityType(Study.class.getName());
		one = (Study) entityServletHelper.createEntity(one, TEST_USER1, null);
		// Now try to re-use the name
		Code two = new Code();
		two.setName("code");
		two.setParentId(one.getId());
		two.setEntityType(Code.class.getName());
		try{
			two = (Code) entityServletHelper.createEntity(two, TEST_USER1, null);
			fail("Code cannot have a parent of type Study");
		}catch(IllegalArgumentException e){
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf(Code.class.getName()) > 0);
			assertTrue(e.getMessage().indexOf(Study.class.getName()) > 0);
		}
		
	}

	@Test(expected=NotFoundException.class)
	public void testActivityId404() throws Exception{
		Project p = new Project();
		p.setName("Create without entity type");
		p.setEntityType(p.getClass().getName());
		String activityId = "123456789";
		Project clone = (Project) entityServletHelper.createEntity(p, TEST_USER1, activityId);
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
		Project parentClone = (Project) entityServletHelper.createEntity(parent, userName, null);
		String parentId = parentClone.getId();
		toDelete.add(parentId);
		// Create a file entity
		FileEntity file = new FileEntity();
		file.setName("FileName");
		file.setEntityType(FileEntity.class.getName());
		file.setParentId(parentId);
		file.setDataFileHandleId(handleOne.getId());
		// Save it
		file = (FileEntity) entityServletHelper.createEntity(file, userName, null);
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
		file = (FileEntity) entityServletHelper.createNewVersion(userName, file);
		// Validate we are on V3
		assertEquals(new Long(2), file.getVersionNumber());
		// First get the URL for the current version
		URL url = entityServletHelper.getEntityFileURLForCurrentVersion(userName, file.getId(), null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(handleTwo.getKey()) > 0);
		URL urlNoRedirect = entityServletHelper.getEntityFileURLForCurrentVersion(userName, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(handleTwo.getKey()) > 0);
		// Now the first version
		url = entityServletHelper.getEntityFileURLForVersion(userName, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(handleOne.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFileURLForVersion(userName, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(handleOne.getKey()) > 0);
		// Get the preview of the current version
		url = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(userName, file.getId(), null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(previewTwo.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForCurrentVersion(userName, file.getId(), Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(previewTwo.getKey()) > 0);
		// Get the preview of the first version
		url = entityServletHelper.getEntityFilePreviewURLForVersion(userName, file.getId(), 1l, null);
		assertNotNull(url);
		assertTrue("Url did not contain the expected key", url.toString().indexOf(previewOne.getKey()) > 0);
		urlNoRedirect = entityServletHelper.getEntityFilePreviewURLForVersion(userName, file.getId(), 1l, Boolean.FALSE);
		assertNotNull(urlNoRedirect);
		assertTrue("Url did not contain the expected key", urlNoRedirect.toString().indexOf(previewOne.getKey()) > 0);
		
		// Validate that we can get the files handles
		FileHandleResults fhr = entityServletHelper.geEntityFileHandlesForCurrentVersion(userName, file.getId());
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(handleTwo.getId(), fhr.getList().get(0).getId());
		assertEquals(previewTwo.getId(), fhr.getList().get(1).getId());
		// Get the previous version as well
		fhr = entityServletHelper.geEntityFileHandlesForVersion(userName, file.getId(), 1l);
		assertNotNull(fhr);
		assertNotNull(fhr.getList());
		assertEquals(2, fhr.getList().size());
		assertEquals(handleOne.getId(), fhr.getList().get(0).getId());
		assertEquals(previewOne.getId(), fhr.getList().get(1).getId());
	}
	
	@Test
	public void testPLFM_1841() throws Exception{
		// Create a study and then attempt to add a file to the study.
		Study study = new Study();
		study.setName("parentStudy-PLFM-1841");
		study.setEntityType(Study.class.getName());
		study = (Study) entityServletHelper.createEntity(study, userName, null);
		toDelete.add(study.getId());
		// Create a file Entity
		// Create a file entity
		FileEntity file = new FileEntity();
		file.setName("FileName");
		file.setEntityType(FileEntity.class.getName());
		file.setParentId(study.getId());
		file.setDataFileHandleId(handleOne.getId());
		// Save it
		file = (FileEntity) entityServletHelper.createEntity(file, userName, null);
		assertNotNull(file);
		assertNotNull(file.getId());
		toDelete.add(file.getId());
	}

	@Test
	public void testGetEntityHeaderByMd5() throws Exception {

		BatchResults<EntityHeader> results = entityServletHelper.getEntityHeaderByMd5(
				userName, "548c050497fb361742b85e0712b0cc96");
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		Project parent = new Project();
		parent.setName("testGetEntityHeaderByMd5");
		parent.setEntityType(Project.class.getName());
		parent = (Project) entityServletHelper.createEntity(parent, userName, null);
		assertNotNull(parent);
		String parentId = parent.getId();
		toDelete.add(parentId);

		FileEntity file = new FileEntity();
		file.setName("testGetEntityHeaderByMd5 file");
		file.setEntityType(FileEntity.class.getName());
		file.setParentId(parentId);
		file.setDataFileHandleId(handleOne.getId());
		file = (FileEntity) entityServletHelper.createEntity(file, userName, null);
		assertNotNull(file);
		assertNotNull(file.getId());
		toDelete.add(file.getId());

		results = entityServletHelper.getEntityHeaderByMd5(userName, handleOne.getContentMd5());
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults());
		assertEquals(file.getId(), results.getResults().get(0).getId());
	}
}
