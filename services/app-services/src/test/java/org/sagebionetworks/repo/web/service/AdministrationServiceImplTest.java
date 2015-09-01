package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
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
	DBOChangeDAO mockChangeDAO;
	
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
		mockChangeDAO = Mockito.mock(DBOChangeDAO.class);
		adminService = new AdministrationServiceImpl(mockBackupDaemonLauncher, mockObjectTypeSerializer, mockUserManager, mockStackStatusManager, mockMessageSyndication, mockChangeDAO);
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

	@Test (expected=UnauthorizedException.class)
	public void testCreateOrUpdateChangeMessagesUnauthorized() throws UnauthorizedException, NotFoundException {
		adminService.createOrUpdateChangeMessages(nonAdminUserId, new ChangeMessages());
	}

	@Test
	public void testCreateOrUpdateChangeMessagesAuthorized() throws UnauthorizedException, NotFoundException {
		ChangeMessages batch =  new ChangeMessages();
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectEtag("etag");
		message.setObjectId("12345");
		message.setObjectType(ObjectType.ENTITY);
		batch.setList(Arrays.asList(message));
		when(mockChangeDAO.replaceChange(batch.getList())).thenReturn(batch.getList());
		adminService.createOrUpdateChangeMessages(adminUserId, batch);
	}

	@Test
	public void testWaiter() throws Exception {
		final int count = 3;
		ExecutorService executor = Executors.newFixedThreadPool(count);
		final CountDownLatch arrivalCounter = new CountDownLatch(count);
		final CountDownLatch returnCounter = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					arrivalCounter.countDown();
					adminService.waitForTesting(adminUserId, false);
					returnCounter.countDown();
					return null;
				}
			});
		}
		assertTrue(arrivalCounter.await(20, TimeUnit.SECONDS));
		// an extra bit of sleep to make sure all service calls have been made and are waiting
		Thread.sleep(1000);
		assertEquals(count, returnCounter.getCount());
		adminService.waitForTesting(adminUserId, true);
		assertTrue(returnCounter.await(1, TimeUnit.SECONDS));
		executor.shutdown();
		assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
	}
}
