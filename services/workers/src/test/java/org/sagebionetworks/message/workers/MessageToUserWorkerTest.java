package org.sagebionetworks.message.workers;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Date;

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
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class MessageToUserWorkerTest {
	
	ProgressCallback<Message> mockCallback;
	MessageManager mockMessageManager;
	WorkerLogger mockWorkerLogger;
	MessageToUserWorker worker;
	

	@Before
	public void before() {
		mockCallback = Mockito.mock(ProgressCallback.class);
		mockMessageManager = Mockito.mock(MessageManager.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		worker = new MessageToUserWorker();
		ReflectionTestUtils.setField(worker, "messageManager", mockMessageManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);
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
		// call under test
		worker.run(mockCallback, msg);
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
		NotFoundException e = new NotFoundException();
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()), any(org.sagebionetworks.util.ProgressCallback.class))).thenThrow(e);
		// call under test
		worker.run(mockCallback, msg);
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, false);
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
		RuntimeException e = new RuntimeException();
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()), any(org.sagebionetworks.util.ProgressCallback.class))).thenThrow(e);
		try {
			// call under test
			worker.run(mockCallback, msg);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e1) {
			//expected
		}
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, true);
	}

}
