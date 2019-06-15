package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class AclObjectRecordWriterTest {
	@Mock
	private AccessControlListDAO mockAccessControlListDao;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private ProgressCallback mockCallback;
	private AclObjectRecordWriter writer;
	private long id = 123L;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		writer = new AclObjectRecordWriter();
		ReflectionTestUtils.setField(writer, "accessControlListDao", mockAccessControlListDao);
		ReflectionTestUtils.setField(writer, "objectRecordDAO", mockObjectRecordDao);
	}

	@Test
	public void deleteAclTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, id+"", ObjectType.ACCESS_CONTROL_LIST, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockObjectRecordDao, never()).saveBatch(anyList(), anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void invalidChangeMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id+"", ObjectType.PRINCIPAL, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
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
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockAccessControlListDao, times(2)).get(id);
		verify(mockAccessControlListDao, times(2)).getOwnerType(id);
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected, expected)), eq(expected.getJsonClassName()));
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
