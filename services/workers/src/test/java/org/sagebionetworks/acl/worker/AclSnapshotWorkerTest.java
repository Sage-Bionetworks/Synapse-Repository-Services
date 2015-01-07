package org.sagebionetworks.acl.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.audit.dao.ResourceAccessRecordDAO;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class AclSnapshotWorkerTest {
	private AclRecordDAO mockAclRecordDao;
	private ResourceAccessRecordDAO mockResourceAccessRecordDao;
	private DBOChangeDAO mockChangeDao;
	private AccessControlListDAO mockAccessControlListDao;

	@Before
	public void setUp() {
		mockAclRecordDao = Mockito.mock(AclRecordDAO.class);
		mockResourceAccessRecordDao = Mockito.mock(ResourceAccessRecordDAO.class);
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockAccessControlListDao = Mockito.mock(AccessControlListDAO.class);
	}

	@Test
	public void testNonAclChangeMessage() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		Message three = null;
		List<Message> messages = Arrays.asList(one, two, three);
		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		// It should just return the results unchanged
		assertNotNull(results);
		assertEquals(messages, results);
		// Confirm that no DAO has been called
		verify(mockAccessControlListDao, never()).get(Matchers.anyLong());
		verify(mockAccessControlListDao, never()).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, never()).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

	@Test
	public void testCreateACL() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Long timestamp = System.currentTimeMillis();
		Date creationDate = new Date();
		Long id = 123L;
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId("123");
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.CREATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId("789");
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);
		
		List<Message> messages = Arrays.asList(one);
		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.times(2)).get(id);
		verify(mockAccessControlListDao, Mockito.times(1)).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Arrays.asList(aclRecord));
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

	@Test
	public void testBuildAclRecord() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Long timestamp = System.currentTimeMillis();
		Date creationDate = new Date();
		Long id = 123L;
		Long principalId = 456L;
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		ResourceAccess ra1 = new ResourceAccess();
		ResourceAccess ra2 = new ResourceAccess();
		ra1.setPrincipalId(principalId);
		ra2.setPrincipalId(principalId);
		ra1.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		ra2.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD)));
		resourceAccess.addAll(Arrays.asList(ra1, ra2));
		
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);
		
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);

		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId("123");
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.UPDATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId("789");
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);

		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(Arrays.asList(one));

		assertEquals(aclRecord, worker.buildAclRecord(MessageUtils.extractMessageBody(one)));
	}

	@Test
	public void testBuildResourceAccessRecord() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Long timestamp = System.currentTimeMillis();
		Date creationDate = new Date();
		Long id = 123L;
		Long principalId = 456L;
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		ResourceAccess ra1 = new ResourceAccess();
		ResourceAccess ra2 = new ResourceAccess();
		ra1.setPrincipalId(principalId);
		ra2.setPrincipalId(principalId);
		ra1.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		ra2.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD)));
		resourceAccess.addAll(Arrays.asList(ra1, ra2));
		
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId("123");
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.UPDATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId("789");
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);
		
		ResourceAccessRecord raRecord1 = new ResourceAccessRecord();
		ResourceAccessRecord raRecord2 = new ResourceAccessRecord();
		raRecord1.setChangeNumber(null);
		raRecord2.setChangeNumber(null);
		raRecord1.setPrincipalId(principalId);
		raRecord2.setPrincipalId(principalId);
		raRecord1.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		raRecord2.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD)));

		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(Arrays.asList(one));

		Set<ResourceAccessRecord> expected = new HashSet<ResourceAccessRecord>(
				Arrays.asList(raRecord1, raRecord2));
		Set<ResourceAccessRecord> actual = new HashSet<ResourceAccessRecord>(
				worker.buildResourceAccessRecordList(MessageUtils.extractMessageBody(one)));
		assertEquals(expected, actual);
	}

	@Test
	public void testUpdateACL() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Long timestamp = System.currentTimeMillis();
		Date creationDate = new Date();
		Long id = 123L;
		Long principalId = 456L;
		Set<ResourceAccess> resourceAccess = new HashSet<ResourceAccess>();
		ResourceAccess ra1 = new ResourceAccess();
		ResourceAccess ra2 = new ResourceAccess();
		ra1.setPrincipalId(principalId);
		ra2.setPrincipalId(principalId);
		ra1.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		ra2.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD)));
		resourceAccess.addAll(Arrays.asList(ra1, ra2));
		
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId("123");
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.UPDATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId("789");
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);
		
		ResourceAccessRecord raRecord1 = new ResourceAccessRecord();
		ResourceAccessRecord raRecord2 = new ResourceAccessRecord();
		raRecord1.setChangeNumber(null);
		raRecord2.setChangeNumber(null);
		raRecord1.setPrincipalId(principalId);
		raRecord2.setPrincipalId(principalId);
		raRecord1.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		raRecord2.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD)));
		
		List<Message> messages = Arrays.asList(one);
		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.times(2)).get(id);
		verify(mockAccessControlListDao, Mockito.times(1)).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
	}
	
	@Test
	public void testDeleteACL() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.DELETE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag");
		Long timestamp = System.currentTimeMillis();
		Date creationDate = new Date();
		Long id = 123L;
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId("123");
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.DELETE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId("789");
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);
		
		List<Message> messages = Arrays.asList(one);
		// Create the worker
		AclSnapshotWorker worker = createNewAclSnapshotWorker(messages);
		// Make the call
		List<Message> results = worker.call();
		assertNotNull(results);
		assertEquals(messages, results);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.times(2)).get(id);
		verify(mockAccessControlListDao, Mockito.times(1)).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Arrays.asList(aclRecord));
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

	/**
	 * Helper to create a new worker for a list of messages.
	 * @param messages
	 * @return
	 */
	private AclSnapshotWorker createNewAclSnapshotWorker(List<Message> messages) {
		AclSnapshotWorker worker = new AclSnapshotWorker(mockAclRecordDao, mockResourceAccessRecordDao, mockChangeDao, mockAccessControlListDao);
		worker.setWorkerProgress(new WorkerProgress() {
			@Override
			public void progressMadeForMessage(Message message) {}
			
			@Override
			public void retryMessage(Message message, int retryTimeoutInSeconds) {}
		});
		worker.setMessages(messages);
		return worker;
	}

}
