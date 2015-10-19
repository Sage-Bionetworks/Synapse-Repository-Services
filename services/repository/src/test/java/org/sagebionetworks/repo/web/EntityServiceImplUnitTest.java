package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.NodeManager.FileHandleReason;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.EntityServiceImpl;

public class EntityServiceImplUnitTest {
	
	EntityService entityService;
	EntityManager mockEntityManager = null;
	HttpServletRequest mockRequest = null;
	UserManager mockUserManager = null;
	FileHandleManager mockFileHandleManager = null;
	static final Long PRINCIPAL_ID = 101L;
	UserInfo userInfo = null;
	
	@Before
	public void before(){
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		mockRequest = Mockito.mock(HttpServletRequest.class);
		entityService = new EntityServiceImpl(mockUserManager, mockEntityManager, mockFileHandleManager);
		
		userInfo = new UserInfo(false);
		userInfo.setId(PRINCIPAL_ID);
		when(mockUserManager.getUserInfo(PRINCIPAL_ID)).thenReturn(userInfo);
	}
	
	//TODO can this test be deleted or should it be replaced with an equivalent test?
	@Ignore
	@Test
	public void testAggregateUpdate() throws Exception{
	/*  
		List<String> idList = new ArrayList<String>();
		idList.add("201");
//		idList.add("301");
//		idList.add("401");
		String userId = "someUser";
		String parentId = "0";
		Collection<Location> toUpdate = new ArrayList<Location>();
		when(mockEntityManager.aggregateEntityUpdate((UserInfo)any(),eq(parentId), eq(toUpdate))).thenReturn(idList);
		Location existingLocation = new Location();
		existingLocation.setId("201");
		existingLocation.setMd5sum("9ca4d9623b655ba970e7b8173066b58f");
		existingLocation.setPath("somePath");
		when(mockEntityManager.getEntity((UserInfo)any(), eq("201"), eq(Location.class))).thenReturn(existingLocation);
		// Now make the call
		controller.aggregateEntityUpdate(userId, parentId, toUpdate, mockRequest);
    */
	}
	
	@Test
	public void testGetFileRedirectURLForCurrentVersion() throws Exception {
		String entityId = "999";
		String fileHandleId = "111";
		when(mockEntityManager.
				getFileHandleIdForVersion(userInfo, entityId, null, FileHandleReason.FOR_FILE_DOWNLOAD)).
				thenReturn(fileHandleId);
		
		FileEntity fileEntity = new FileEntity();
		String fileNameOverride = "foo.txt";
		fileEntity.setFileNameOverride(fileNameOverride);
		when(mockEntityManager.getEntitySecondaryFields(userInfo, entityId, FileEntity.class)).thenReturn(fileEntity);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(fileHandleId, "foo.txt")).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForCurrentVersion(PRINCIPAL_ID, entityId));
	}
	
	
	@Test
	public void testGetFileRedirectURLForVersion() throws Exception {
		String entityId = "999";
		String fileHandleId = "111";
		Long version = 1L;
		when(mockEntityManager.
				getFileHandleIdForVersion(userInfo, entityId, version, FileHandleReason.FOR_FILE_DOWNLOAD)).
				thenReturn(fileHandleId);
		
		FileEntity fileEntity = new FileEntity();
		String fileNameOverride = "foo.txt";
		fileEntity.setFileNameOverride(fileNameOverride);
		when(mockEntityManager.getEntitySecondaryFieldsForVersion(userInfo, entityId, version, FileEntity.class)).thenReturn(fileEntity);
		String url = "http://foo.bar";
		when(mockFileHandleManager.getRedirectURLForFileHandle(fileHandleId, "foo.txt")).thenReturn(url);
		assertEquals(url, entityService.getFileRedirectURLForVersion(PRINCIPAL_ID, entityId, version));
	}
	
}
