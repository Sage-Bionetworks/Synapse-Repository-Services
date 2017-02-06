package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.StackStatusManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.message.MessageSyndication;
import org.sagebionetworks.repo.model.AnnotationNameSpace;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.admin.FileUpdateRequest;
import org.sagebionetworks.repo.model.admin.FileUpdateResult;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for AdministrationServiceImpl
 * 
 * @author John
 *
 */
public class AdministrationServiceImplTest {

	@Mock
	BackupDaemonLauncher mockBackupDaemonLauncher;
	@Mock
	ObjectTypeSerializer mockObjectTypeSerializer;
	@Mock
	UserManager mockUserManager;
	@Mock
	StackStatusManager mockStackStatusManager;
	@Mock
	MessageSyndication mockMessageSyndication;
	@Mock
	DBOChangeDAO mockChangeDAO;
	@Mock
	EntityManager mockEntityManager;
	@Mock
	FileHandleDao mockFileHandleDao;
	@Mock
	IdGenerator mockIdGenerator;
	@Mock
	NodeDAO mockNodeDao;
	
	AdministrationServiceImpl adminService;
	
	Long nonAdminUserId = 98345L;
	UserInfo nonAdmin;
	Long adminUserId = 842059834L;
	UserInfo admin;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		MockitoAnnotations.initMocks(this);
		adminService = new AdministrationServiceImpl();
		ReflectionTestUtils.setField(adminService, "backupDaemonLauncher", mockBackupDaemonLauncher);
		ReflectionTestUtils.setField(adminService, "objectTypeSerializer", mockObjectTypeSerializer);
		ReflectionTestUtils.setField(adminService, "userManager", mockUserManager);
		ReflectionTestUtils.setField(adminService, "stackStatusManager", mockStackStatusManager);
		ReflectionTestUtils.setField(adminService, "messageSyndication", mockMessageSyndication);
		ReflectionTestUtils.setField(adminService, "changeDAO", mockChangeDAO);
		ReflectionTestUtils.setField(adminService, "entityManager", mockEntityManager);
		ReflectionTestUtils.setField(adminService, "fileHandleDao", mockFileHandleDao);
		ReflectionTestUtils.setField(adminService, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(adminService, "nodeDao", mockNodeDao);
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

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateFileWithNullUser() {
		adminService.updateFile(null, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateFileWithNonAdmin() {
		adminService.updateFile(nonAdminUserId, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateFileWithNullRequest() {
		adminService.updateFile(adminUserId, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateFileWithNullEntityId() {
		adminService.updateFile(adminUserId, new FileUpdateRequest());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateFileWithNullVersion() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setEtag("etag");
		adminService.updateFile(adminUserId, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateFileWithNullEtag() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setVersion(1L);
		adminService.updateFile(adminUserId, request);
	}

	@Test
	public void testUpdateFileNotFound() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setVersion(2L);
		request.setEtag("etag");
		when(mockEntityManager.<Entity>getEntityForVersion(admin, "1", 2L, FileEntity.class))
				.thenThrow(new NotFoundException());
		FileUpdateResult result = adminService.updateFile(adminUserId, request);
		assertFalse(result.getIsSuccessful());
		assertEquals("1.2 cannot be found.", result.getReason());
	}

	@Test
	public void testUpdateFileWithoutFileNameOverride() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setVersion(2L);
		request.setEtag("etag");
		when(mockEntityManager.<Entity>getEntityForVersion(admin, "1", 2L, FileEntity.class))
				.thenReturn(new FileEntity());
		FileUpdateResult result = adminService.updateFile(adminUserId, request);
		assertFalse(result.getIsSuccessful());
		assertEquals("1.2 does not have fileNameOverride field set.", result.getReason());
	}

	@Test
	public void testUpdateFileConflicted() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setVersion(2L);
		request.setEtag("entityEtag");
		FileEntity entity = new FileEntity();
		entity.setId("1");
		entity.setFileNameOverride("fileNameOverride");
		entity.setDataFileHandleId("3");
		entity.setEtag("entityEtag");
		when(mockEntityManager.<Entity>getEntityForVersion(admin, "1", 2L, FileEntity.class))
				.thenReturn(entity);
		when(mockNodeDao.lockNodeAndIncrementEtag("1", "entityEtag"))
				.thenThrow(new ConflictingUpdateException());
		FileUpdateResult result = adminService.updateFile(adminUserId, request);
		assertFalse(result.getIsSuccessful());
		assertEquals("Cannot get a lock for 1.2", result.getReason());
	}

	@Test
	public void testUpdateFileWithFileNameOverride() {
		FileUpdateRequest request = new FileUpdateRequest();
		request.setEntityId("1");
		request.setVersion(2L);
		request.setEtag("entityEtag");

		FileEntity entity = new FileEntity();
		entity.setId("1");
		entity.setFileNameOverride("fileNameOverride");
		entity.setDataFileHandleId("3");
		entity.setEtag("entityEtag");
		when(mockEntityManager.<Entity>getEntityForVersion(admin, "1", 2L, FileEntity.class))
				.thenReturn(entity);

		S3FileHandle toCopy = new S3FileHandle();
		toCopy.setId("4");
		toCopy.setCreatedBy("createdBy");
		toCopy.setCreatedOn(new Date());
		toCopy.setFileName("fileName");
		toCopy.setEtag("etag");
		when(mockFileHandleDao.get("3")).thenReturn(toCopy);

		when(mockIdGenerator.generateNewId(TYPE.FILE_IDS)).thenReturn(5L);

		NamedAnnotations annotations = new NamedAnnotations();
		Annotations primary = new Annotations();
		primary.addAnnotation("fileNameOverride", "fileNameOverride");
		annotations.put(AnnotationNameSpace.PRIMARY, primary );
		when(mockNodeDao.getAnnotationsForVersion("1", 2L)).thenReturn(annotations);

		FileUpdateResult result = adminService.updateFile(adminUserId, request);
		assertTrue(result.getIsSuccessful());
		assertNull(result.getReason());

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockFileHandleDao).createBatch(captor.capture());
		List<FileHandle> captured = captor.getValue();
		assertNotNull(captured);
		assertEquals(1, captured.size());
		FileHandle copied = captured.get(0);
		assertEquals("5", copied.getId());
		assertEquals(toCopy.getCreatedBy(), copied.getCreatedBy());
		assertEquals(toCopy.getCreatedOn(), copied.getCreatedOn());
		assertEquals("fileNameOverride", copied.getFileName());
		assertFalse(copied.getEtag().equals("etag"));

		verify(mockNodeDao).lockNodeAndIncrementEtag("1", "entityEtag");
		ArgumentCaptor<NamedAnnotations> annotationCaptor = ArgumentCaptor.forClass(NamedAnnotations.class);
		verify(mockNodeDao).replaceVersion(eq("1"), eq(2L), annotationCaptor.capture(), eq("5"));
		NamedAnnotations newAnnotations = annotationCaptor.getValue();
		assertNotNull(newAnnotations);
		assertNull(newAnnotations.getPrimaryAnnotations().getAllValues("fileNameOverride"));
	}
}
