package org.sagebionetworks.message.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

public class MessageToUserWorkerTest {
	List<Message> messages;
	MessageManager mockMessageManager;
	WorkerLogger mockWorkerLogger;


	@Before
	public void before() {
		messages = new LinkedList<Message>();
		mockMessageManager = Mockito.mock(MessageManager.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
	}
	
	@Test
	public void testSkipNonMessageMsg() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("id");
		chgMsg.setObjectType(ObjectType.ENTITY);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		MessageToUserWorker worker = new MessageToUserWorker(messages, mockMessageManager, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		assertNotNull(processedMsgs);
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
	}

	@Test
	public void testWorkerLoggerCalledOnNotFound() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.MESSAGE);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		NotFoundException e = new NotFoundException();
		when(mockMessageManager.processMessage(chgMsg.getObjectId())).thenThrow(e);
		MessageToUserWorker worker = new MessageToUserWorker(messages, mockMessageManager, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, false);
		assertNotNull(processedMsgs);
		assertEquals(1, processedMsgs.size());
		assertEquals(msg, processedMsgs.get(0));
	}

	@Test
	public void testWorkerLoggerCalledOnThrowable() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectEtag("etag");
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.MESSAGE);
		chgMsg.setParentId("parentId");
		chgMsg.setTimestamp(new Date());
		Message msg = MessageUtils.createMessage(chgMsg, "outerId1000", "handler");
		messages.add(msg);
		RuntimeException e = new RuntimeException();
		when(mockMessageManager.processMessage(chgMsg.getObjectId())).thenThrow(e);
		MessageToUserWorker worker = new MessageToUserWorker(messages, mockMessageManager, mockWorkerLogger);
		List<Message> processedMsgs = worker.call();
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, true);
		assertNotNull(processedMsgs);
		assertEquals(0, processedMsgs.size());
	}

}
