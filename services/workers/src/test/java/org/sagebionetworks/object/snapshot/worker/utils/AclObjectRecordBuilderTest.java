package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class AclObjectRecordBuilderTest {
	
	private AccessControlListDAO mockAccessControlListDao;
	private AclObjectRecordBuilder builder;
	private long id = 123L;

	@Before
	public void setup() {
		mockAccessControlListDao = Mockito.mock(AccessControlListDAO.class);
		builder = new AclObjectRecordBuilder(mockAccessControlListDao);
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteAclTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, id+"", ObjectType.ACCESS_CONTROL_LIST, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void invalidChangeMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id+"", ObjectType.PRINCIPAL, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}
	
	@Test
	public void validChangeMessageTest() throws IOException {
		AccessControlList acl = new AccessControlList();
		acl.setEtag("etag");
		Mockito.when(mockAccessControlListDao.get(id)).thenReturn(acl);
		
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id+"", ObjectType.ACCESS_CONTROL_LIST, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(acl, changeMessage.getTimestamp().getTime());
		ObjectRecord actual = builder.build(changeMessage);
		Mockito.verify(mockAccessControlListDao).get(id);
		assertEquals(expected, actual);
	}
}
