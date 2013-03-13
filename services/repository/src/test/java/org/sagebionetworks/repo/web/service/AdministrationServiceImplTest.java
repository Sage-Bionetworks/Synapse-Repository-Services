package org.sagebionetworks.repo.web.service;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ObjectType;
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
	DependencyManager mockDependencyManager;
	MessageSyndication mockMessageSyndication;
	AdministrationServiceImpl adminService;
	
	String nonAdminUserId = "nonAdminUser";
	UserInfo nonAdmin;
	String adminUserId = "AdminUser";
	UserInfo admin;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		mockBackupDaemonLauncher = Mockito.mock(BackupDaemonLauncher.class);
		mockObjectTypeSerializer = Mockito.mock(ObjectTypeSerializer.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockStackStatusManager = Mockito.mock(StackStatusManager.class);
		mockDependencyManager = Mockito.mock(DependencyManager.class);
		mockMessageSyndication = Mockito.mock(MessageSyndication.class);
		adminService = new AdministrationServiceImpl(mockBackupDaemonLauncher, mockObjectTypeSerializer, mockUserManager, mockStackStatusManager, mockDependencyManager, mockMessageSyndication);
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
}
