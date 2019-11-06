package org.sagebionetworks.message.workers;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

public class MessageToUserWorkerTest {
	
	ProgressCallback mockCallback;
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
		chgMsg.setObjectId("id");
		chgMsg.setObjectType(ObjectType.ENTITY);
		chgMsg.setTimestamp(new Date());
		// call under test
		worker.run(mockCallback, chgMsg);
	}

	@Test
	public void testWorkerLoggerCalledOnNotFound() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.MESSAGE);
		chgMsg.setTimestamp(new Date());
		NotFoundException e = new NotFoundException();
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()), any(org.sagebionetworks.common.util.progress.ProgressCallback.class))).thenThrow(e);
		// call under test
		worker.run(mockCallback, chgMsg);
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, false);
	}

	@Test
	public void testWorkerLoggerCalledOnThrowable() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.MESSAGE);
		chgMsg.setTimestamp(new Date());
		RuntimeException e = new RuntimeException();
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()), any(org.sagebionetworks.common.util.progress.ProgressCallback.class))).thenThrow(e);
		try {
			// call under test
			worker.run(mockCallback, chgMsg);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e1) {
			//expected
		}
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, true);
	}

}
