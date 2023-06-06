package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AbstractAwsKinesisLogRecord;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.audit.KinesisJsonEntityRecord;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;

@ExtendWith(MockitoExtension.class)
public class AclObjectRecordWriterTest {
	@Mock
	private AccessControlListDAO mockAccessControlListDao;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private AwsKinesisFirehoseLogger logger;
	
	@InjectMocks
	private AclObjectRecordWriter writer;
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;
	private long id = 123L;

	@Test
	public void deleteAclTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, id+"", ObjectType.ACCESS_CONTROL_LIST, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockObjectRecordDao, never()).saveBatch(anyList(), anyString());
	}
	
	@Test
	public void invalidChangeMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id+"", ObjectType.PRINCIPAL, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {	
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
	}
	
	@Test
	public void validChangeMessageTest() throws IOException {
		AccessControlList acl = new AccessControlList();
		acl.setEtag("etag");
		when(mockAccessControlListDao.get(id)).thenReturn(acl);
		when(mockAccessControlListDao.getOwnerType(id)).thenReturn(ObjectType.ENTITY);
		
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id+"", ObjectType.ACCESS_CONTROL_LIST, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		AclRecord record = AclObjectRecordWriter.buildAclRecord(acl, ObjectType.ENTITY);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(record, changeMessage.getTimestamp().getTime());
		KinesisObjectSnapshotRecord<?> expectedRecordOne =KinesisObjectSnapshotRecord.map(changeMessage, record);
		KinesisObjectSnapshotRecord<?> expectedRecordTwo =KinesisObjectSnapshotRecord.map(changeMessage, record);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockAccessControlListDao, times(2)).get(id);
		verify(mockAccessControlListDao, times(2)).getOwnerType(id);
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected, expected)), eq(expected.getJsonClassName()));
		verify(logger).logBatch(eq("aclSnapshots"), recordCaptor.capture());
		assertNotNull(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		expectedRecordOne.withSnapshotTimestamp(recordCaptor.getAllValues().get(0).get(0).getSnapshotTimestamp());
		assertNotNull(recordCaptor.getAllValues().get(0).get(1).getSnapshotTimestamp());
		expectedRecordTwo.withSnapshotTimestamp(recordCaptor.getAllValues().get(0).get(1).getSnapshotTimestamp());
		assertEquals(List.of(expectedRecordOne, expectedRecordTwo), recordCaptor.getValue());
	}

	@Test
	public void buildAclRecordTest() {
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy("createdBy");
		acl.setCreationDate(new Date(0));
		acl.setEtag("etag");
		acl.setId("id");
		acl.setModifiedBy("modifiedBy");
		acl.setModifiedOn(new Date());
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		AclRecord record = AclObjectRecordWriter.buildAclRecord(acl, ObjectType.EVALUATION);
		assertEquals(ObjectType.EVALUATION, record.getOwnerType());
		assertEquals(acl.getCreatedBy(), record.getCreatedBy());
		assertEquals(acl.getCreationDate(), record.getCreationDate());
		assertEquals(acl.getEtag(), record.getEtag());
		assertEquals(acl.getModifiedBy(), record.getModifiedBy());
		assertEquals(acl.getModifiedOn(), record.getModifiedOn());
		assertEquals(acl.getId(), record.getId());
		assertEquals(acl.getResourceAccess(), record.getResourceAccess());
	}
}
