package org.sagebionetworks.message.workers;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class MessageToUserWorkerTest {
	
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private WorkerLogger mockWorkerLogger;
	
	@InjectMocks
	private MessageToUserWorker worker;
	
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
		
		verifyZeroInteractions(mockMessageManager);
	}

	@Test
	public void testWorkerLoggerCalledOnNotFound() throws Exception {
		ChangeMessage chgMsg = new ChangeMessage();
		chgMsg.setChangeNumber(1000L);
		chgMsg.setChangeType(ChangeType.CREATE);
		chgMsg.setObjectId("12345");
		chgMsg.setObjectType(ObjectType.MESSAGE);
		chgMsg.setTimestamp(new Date());
		NotFoundException e = new NotFoundException("");
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()))).thenThrow(e);
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
		when(mockMessageManager.processMessage(eq(chgMsg.getObjectId()))).thenThrow(e);
		
		assertThrows(RecoverableMessageException.class, () -> {			
			// call under test
			worker.run(mockCallback, chgMsg);
		});
		
		verify(mockWorkerLogger).logWorkerFailure(MessageToUserWorker.class, chgMsg, e, true);
	}

}
