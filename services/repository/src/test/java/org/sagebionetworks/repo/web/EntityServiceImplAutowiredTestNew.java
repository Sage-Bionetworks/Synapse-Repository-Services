package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityServiceImplAutowiredTestNew {

	@Autowired
	EntityService entityService;
	
	@Autowired
	UserProvider testUserProvider;
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	Project project;
	List<String> toDelete;
	HttpServletRequest mockRequest;
	String userName;
	UserInfo userInfo;
	
	S3FileHandle fileHandle1;
	S3FileHandle fileHandle2;
	
	@Before
	public void before() throws Exception{
		toDelete = new LinkedList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		userInfo = testUserProvider.getTestAdminUserInfo();
		UserInfo.validateUserInfo(userInfo);
		userName = userInfo.getUser().getUserId();
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		// Create a project
		project = new Project();
		project = entityService.createEntity(userName, project, null, mockRequest);
		toDelete.add(project.getId());
		
		// Create some file handles
		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName("bucket");
		handle.setKey("key");
		handle.setCreatedBy(userInfo.getIndividualGroup().getId());
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
					entityService.deleteEntity(userName, id);
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
		file = entityService.createEntity(userName, file, null, mockRequest);
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
		file = entityService.createEntity(userName, file, null, mockRequest);
		assertNotNull(file);
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(userName, file, false, null, mockRequest);
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
		file = entityService.createEntity(userName, file, null, mockRequest);
		assertNotNull(file);
		// Now try to set it to null
		file.setDataFileHandleId(null);
		file = entityService.updateEntity(userName, file, false, null, mockRequest);
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
		file = entityService.createEntity(userName, file, null, mockRequest);
		assertNotNull(file);
		assertEquals("Should start off as version one",new Long(1), file.getVersionNumber());
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(userName, file, false, null, mockRequest);
		// This should trigger a version change.
		assertEquals("Changing the dataFileHandleId of a FileEntity should have created a new version",new Long(2), file.getVersionNumber());
		// Now make sure if we change the name but the file
		file.setName("newName");
		file = entityService.updateEntity(userName, file, false, null, mockRequest);
		assertEquals("A new version should not have been created when a name changed",new Long(2), file.getVersionNumber());
	}
}
