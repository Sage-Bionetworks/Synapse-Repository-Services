package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class FileHandleAuthorizationManagerImplTest {
	
	@Mock
	private FileHandleDao fileHandleDao;
	
	@InjectMocks
	FileHandleAuthorizationManagerImpl manager;
	
	private UserInfo userInfo;
	private UserInfo adminUser;
	
	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L);
		adminUser = new UserInfo(true, 455L);
	}

	@Test
	public void testCanAccessRawFileHandleById() throws NotFoundException{
		// The admin can access anything
		String creator = userInfo.getId().toString();
		String fileHandlId = "3333";
		when(fileHandleDao.getHandleCreator(fileHandlId)).thenReturn(creator);
		assertTrue(manager.canAccessRawFileHandleById(adminUser, fileHandlId).isAuthorized(), "Admin should have access to all FileHandles");
		assertTrue(manager.canAccessRawFileHandleById(userInfo, fileHandlId).isAuthorized(), "Creator should have access to their own FileHandles");
		// change the users id
		UserInfo notTheCreatoro = new UserInfo(false, "999999");
		assertFalse(manager.canAccessRawFileHandleById(notTheCreatoro, fileHandlId).isAuthorized(), "Only the creator (or admin) should have access a FileHandle");
		verify(fileHandleDao, times(2)).getHandleCreator(fileHandlId);
	}

}
