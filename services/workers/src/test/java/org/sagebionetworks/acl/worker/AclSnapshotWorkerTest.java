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
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

public class AclSnapshotWorkerTest {
	private AclRecordDAO mockAclRecordDao;
	private ResourceAccessRecordDAO mockResourceAccessRecordDao;
	private DBOChangeDAO mockChangeDao;
	private AccessControlListDAO mockAccessControlListDao;
	private ProgressCallback<Message> mockProgressCallback;
	AclSnapshotWorker worker;

	@Before
	public void setUp() {
		mockAclRecordDao = Mockito.mock(AclRecordDAO.class);
		mockResourceAccessRecordDao = Mockito.mock(ResourceAccessRecordDAO.class);
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockAccessControlListDao = Mockito.mock(AccessControlListDAO.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		worker = new AclSnapshotWorker(mockAclRecordDao, mockResourceAccessRecordDao, mockChangeDao, mockAccessControlListDao);
	}

	@Test
	public void testNonAclChangeMessage() throws Exception {
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACTIVITY, "etag");
		Message two = MessageUtils.buildMessage(ChangeType.CREATE, "456", ObjectType.ACTIVITY, "etag");
		List<Message> messages = Arrays.asList(one, two);
		// Make the call
		worker.run(mockProgressCallback, one);
		worker.run(mockProgressCallback, two);
		// Confirm that no DAO has been called
		verify(mockAccessControlListDao, never()).get(Matchers.anyLong());
		verify(mockAccessControlListDao, never()).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, never()).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

	@Test
	public void testCreateACL() throws Exception {
		Long timestamp = System.currentTimeMillis();
		Message one = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag", timestamp);
		Date creationDate = new Date(timestamp);
		Long id = 123L;
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setEtag("etag");
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);

		List<Message> messages = Arrays.asList(one);
		// Make the call
		worker.run(mockProgressCallback, one);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.times(1)).get(id);
		verify(mockAccessControlListDao, Mockito.times(1)).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

	@Test
	public void testBuildAclRecord() throws Exception {
		Long timestamp = System.currentTimeMillis();
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag", timestamp);
		Date creationDate = new Date(timestamp);
		Long id = 123L;
		String ownerId = "789";
		Set<ResourceAccess> resourceAccess =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(456L, 654L), 2, false);
		AccessControlList acl = new AccessControlList();
		acl.setId(ownerId);
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);

		Mockito.when(mockAccessControlListDao.get(id)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(id)).thenReturn(ObjectType.ENTITY);

		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId(id.toString());
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.UPDATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId(ownerId);
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);

		// Create the worker
		assertEquals(aclRecord, worker.buildAclRecord(MessageUtils.extractMessageBody(one), acl));
	}

	@Test
	public void testBuildResourceAccessRecord() throws Exception {
		Long timestamp = System.currentTimeMillis();
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag", timestamp);
		Date creationDate = new Date(timestamp);
		Long id = 123L;
		String ownerId = "789";
		Long principalId1 = 456L;
		Long principalId2 = 654L;
		Set<ResourceAccess> resourceAccess =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(principalId1, principalId2), 2, false);
		AccessControlList acl = new AccessControlList();
		acl.setId(ownerId);
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);
		Mockito.when(mockAccessControlListDao.get(id)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(id)).thenReturn(ObjectType.ENTITY);
		
		AclRecord aclRecord = new AclRecord();
		aclRecord.setAclId(id.toString());
		aclRecord.setChangeNumber(null);
		aclRecord.setChangeType(ChangeType.UPDATE);
		aclRecord.setCreationDate(creationDate);
		aclRecord.setEtag("etag");
		aclRecord.setOwnerId(ownerId);
		aclRecord.setOwnerType(ObjectType.ENTITY);
		aclRecord.setTimestamp(timestamp);

		Set<ResourceAccessRecord> expected = AclSnapshotWorkerTestUtils.createSetOfResourceAccessRecord(resourceAccess);
		Set<ResourceAccessRecord> actual = new HashSet<ResourceAccessRecord>(
				worker.buildResourceAccessRecordList(MessageUtils.extractMessageBody(one), acl));
		assertEquals(expected, actual);
	}

	@Test
	public void testUpdateACL() throws Exception {
		Long timestamp = System.currentTimeMillis();
		Message one = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag", timestamp );
		Date creationDate = new Date(timestamp);
		Long id = 123L;
		Set<ResourceAccess> resourceAccess =
				AclSnapshotWorkerTestUtils.createSetOfResourceAccess(Arrays.asList(456L, 654L), 2, false);

		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(resourceAccess);
		acl.setEtag("etag");
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		// Make the call
		worker.run(mockProgressCallback, one);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.times(1)).get(id);
		verify(mockAccessControlListDao, Mockito.times(1)).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
	}

	@Test
	public void testDeleteACL() throws Exception {
		Long timestamp = System.currentTimeMillis();
		Message one = MessageUtils.buildMessage(ChangeType.DELETE, "123", ObjectType.ACCESS_CONTROL_LIST, "etag", timestamp);
		Date creationDate = new Date(timestamp);
		Long id = 123L;
		AccessControlList acl = new AccessControlList();
		acl.setId("789"); // ownerId
		acl.setCreationDate(creationDate);
		acl.setEtag("etag");
		Mockito.when(mockAccessControlListDao.get(123L)).thenReturn(acl);
		Mockito.when(mockAccessControlListDao.getOwnerType(123L)).thenReturn(ObjectType.ENTITY);
		
		List<Message> messages = Arrays.asList(one);
		// Make the call
		worker.run(mockProgressCallback, one);
		// confirm that the DAOs have been called
		verify(mockAccessControlListDao, Mockito.never()).get(id);
		verify(mockAccessControlListDao, Mockito.never()).getOwnerType(Matchers.anyLong());
		verify(mockAclRecordDao, Mockito.times(1)).saveBatch(Matchers.anyList());
		verify(mockResourceAccessRecordDao, never()).saveBatch(Matchers.anyList());
	}

}
