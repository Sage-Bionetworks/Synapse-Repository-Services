package org.sagebionetworks.repo.web;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


public class EntityServiceImplAutowiredTestNew extends AbstractAutowiredControllerTestBase {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	private Project project;
	private List<String> toDelete;
	private HttpServletRequest mockRequest;
	private Long adminUserId;
	private UserInfo adminUserInfo;
	
	private S3FileHandle fileHandle1;
	private S3FileHandle fileHandle2;
	
	@Before
	public void before() throws Exception{
		toDelete = new LinkedList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		UserInfo.validateUserInfo(adminUserInfo);
		
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		// Create a project
		project = new Project();
		project = entityService.createEntity(adminUserId, project, null, mockRequest);
		toDelete.add(project.getId());
		
		// Create some file handles
		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName("bucket");
		handle.setKey("key");
		handle.setCreatedBy(adminUserInfo.getId().toString());
		handle.setCreatedOn(new Date());
		handle.setContentSize(123l);
		handle.setConcreteType("text/plain");
		handle.setEtag("etag");
		handle.setFileName("foo.bar");
		handle.setContentMd5("md5");
		
		fileHandle1 = fileHandleDao.createFile(handle);
		handle.setKey("key2");
		handle.setFileName("two.txt");
		fileHandle2 = fileHandleDao.createFile(handle);
	}
	@After
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					entityService.deleteEntity(adminUserId, id);
				} catch (Exception e) {	}
			}
		}
		if(fileHandle1 != null){
			fileHandleDao.delete(fileHandle1.getId());
		}
		if(fileHandle2 != null){
			fileHandleDao.delete(fileHandle2.getId());
		}
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testPLFM_1754CreateNullFileHandleId() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file = entityService.createEntity(adminUserId, file, null, mockRequest);
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test
	public void testPLFM_1754HappyCase() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		file = entityService.createEntity(adminUserId, file, null, mockRequest);
		assertNotNull(file);
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(adminUserId, file, false, null, mockRequest);
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testPLFM_1754UpdateNull() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		file = entityService.createEntity(adminUserId, file, null, mockRequest);
		assertNotNull(file);
		// Now try to set it to null
		file.setDataFileHandleId(null);
		file = entityService.updateEntity(adminUserId, file, false, null, mockRequest);
	}
	
	/**
	 * PLFM-1744 "Any change to a FileEntity's 'dataFileHandleId' should trigger a new version."
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testPLFM_1744() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		file = entityService.createEntity(adminUserId, file, null, mockRequest);
		assertNotNull(file);
		assertEquals("Should start off as version one",new Long(1), file.getVersionNumber());
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(adminUserId, file, false, null, mockRequest);
		// This should trigger a version change.
		assertEquals("Changing the dataFileHandleId of a FileEntity should have created a new version",new Long(2), file.getVersionNumber());
		// Now make sure if we change the name but the file
		file.setName("newName");
		file = entityService.updateEntity(adminUserId, file, false, null, mockRequest);
		assertEquals("A new version should not have been created when a name changed",new Long(2), file.getVersionNumber());
	}

	@Test
	public void testProjectAlias() {
		String alias1 = "alias" + RandomUtils.nextInt();
		String alias2 = "alias" + RandomUtils.nextInt();
		String alias3 = "alias" + RandomUtils.nextInt();
		// create alias1
		Project project1 = new Project();
		project1.setName("project" + UUID.randomUUID());
		project1.setAlias(alias1);
		project1 = entityService.createEntity(adminUserId, project1, null, mockRequest);
		toDelete.add(project1.getId());
		assertEquals(alias1, ((Project) entityService.getEntity(adminUserId, project1.getId(), mockRequest)).getAlias());
		// create alias2
		Project project2 = new Project();
		project2.setName("project" + UUID.randomUUID());
		project2.setAlias(alias2);
		project2 = entityService.createEntity(adminUserId, project2, null, mockRequest);
		toDelete.add(project2.getId());
		assertEquals(alias2, ((Project) entityService.getEntity(adminUserId, project2.getId(), mockRequest)).getAlias());
		// fail on create alias1
		Project projectFailCreate = new Project();
		projectFailCreate.setName("project" + UUID.randomUUID());
		projectFailCreate.setAlias(alias1);
		try {
			entityService.createEntity(adminUserId, projectFailCreate, null, mockRequest);
			fail("duplicate entry should have been rejected");
		} catch (IllegalArgumentException e) {
			assertEquals(DuplicateKeyException.class, e.getCause().getClass());
		}
		// update to null
		project2.setAlias(null);
		project2 = entityService.updateEntity(adminUserId, project2, false, null, mockRequest);
		assertNull(((Project) entityService.getEntity(adminUserId, project2.getId(), mockRequest)).getAlias());
		// fail on update to alias1
		try {
			project2.setAlias(alias1);
			entityService.updateEntity(adminUserId, project2, false, null, mockRequest);
			fail("duplicate entry should have been rejected");
		} catch (IllegalArgumentException e) {
			assertEquals(DuplicateKeyException.class, e.getCause().getClass());
		}
		project2.setAlias(alias3);
		project2 = entityService.updateEntity(adminUserId, project2, false, null, mockRequest);
		assertEquals(alias3, ((Project) entityService.getEntity(adminUserId, project2.getId(), mockRequest)).getAlias());
		// create alias2 again
		Project project2Again = new Project();
		project2Again.setName("project" + UUID.randomUUID());
		project2Again.setAlias(alias2);
		project2Again = entityService.createEntity(adminUserId, project2Again, null, mockRequest);
		toDelete.add(project2Again.getId());
		assertEquals(alias2, ((Project) entityService.getEntity(adminUserId, project2Again.getId(), mockRequest)).getAlias());
	}
}
