package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;

/**
 * Unit test for AdministrationServiceImpl
 * 
 * @author John
 *
 */
public class AdministrationServiceImplTest {
	

	BackupDaemonLauncher mockBackupDaemonLauncher;	
	ObjectTypeSerializer mockObjectTypeSerializer;	
	UserManager mockUserManager;
	StackStatusManager mockStackStatusManager;	
	MessageSyndication mockMessageSyndication;
	AdministrationServiceImpl adminService;
	
	Long nonAdminUserId = 98345L;
	UserInfo nonAdmin;
	Long adminUserId = 842059834L;
	UserInfo admin;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockBackupDaemonLauncher = Mockito.mock(BackupDaemonLauncher.class);
		mockObjectTypeSerializer = Mockito.mock(ObjectTypeSerializer.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockStackStatusManager = Mockito.mock(StackStatusManager.class);
		mockMessageSyndication = Mockito.mock(MessageSyndication.class);
		adminService = new AdministrationServiceImpl(mockBackupDaemonLauncher, mockObjectTypeSerializer, mockUserManager, mockStackStatusManager, mockMessageSyndication);
		// Setup the users
		nonAdmin = new UserInfo(false);
		admin = new UserInfo(true);
		when(mockUserManager.getUserInfo(nonAdminUserId)).thenReturn(nonAdmin);
		when(mockUserManager.getUserInfo(adminUserId)).thenReturn(admin);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testListChangeMessagesUnauthorized() throws DatastoreException, NotFoundException{
		adminService.listChangeMessages(nonAdminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}

	@Test 
	public void testListChangeMessagesAuthorized() throws DatastoreException, NotFoundException{
		adminService.listChangeMessages(adminUserId, 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testRebroadcastChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		adminService.rebroadcastChangeMessagesToQueue(nonAdminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}

	@Test 
	public void testRebroadcastChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		adminService.rebroadcastChangeMessagesToQueue(adminUserId, "queuename", 0l, ObjectType.ACTIVITY, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testReFireChangeMessagesToQueueUnauthorized() throws DatastoreException, NotFoundException{
		adminService.reFireChangeMessages(nonAdminUserId, 0L, Long.MAX_VALUE);
	}

	@Test 
	public void testReFireChangeMessagesToQueueAuthorized() throws DatastoreException, NotFoundException{
		adminService.reFireChangeMessages(adminUserId, 0L, Long.MAX_VALUE);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetCurrentChangeNumberUnauthorized() throws DatastoreException, NotFoundException{
		adminService.getCurrentChangeNumber(nonAdminUserId);
	}

	@Test 
	public void testGetCurrentChangeNumberAuthorized() throws DatastoreException, NotFoundException{
		adminService.getCurrentChangeNumber(adminUserId);
	}
	
}
