package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.sqs.model.Message;

public class NodeObjectRecordBuilderTest {

	private NodeDAO mockNodeDAO;
	private NodeObjectRecordBuilder builder;
	private Node node;

	@Before
	public void setup() {
		mockNodeDAO = Mockito.mock(NodeDAO.class);
		builder = new NodeObjectRecordBuilder(mockNodeDAO);
		node = new Node();
		node.setId("123");
		Mockito.when(mockNodeDAO.getNode("123")).thenReturn(node);
	}

	@Test (expected=IllegalArgumentException.class)
	public void deleteChangeMessage() {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, "123", ObjectType.ENTITY, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test (expected=IllegalArgumentException.class)
	public void invalidObjectType() {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		builder.build(changeMessage);
	}

	@Test
	public void validChangeMessage() {
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);
		ObjectRecord actual = builder.build(changeMessage);
		Mockito.verify(mockNodeDAO).getNode(Mockito.eq("123"));
		assertEquals(expected, actual);
	}
}
