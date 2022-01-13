package org.sagebionetworks.ses.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.ses.SESNotificationManager;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class SESNotificationWorkerTest {

	@Mock
	private WorkerLogger mockLogger;

	@Mock
	private SESNotificationManager mockManager;

	@Mock
	private SESJsonNotification mockNotification;

	@InjectMocks
	private SESNotificationWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private Message mockMessage;
	
	private String messageBody = "message body";
	
	@BeforeEach
	public void before() {
		when(mockMessage.getBody()).thenReturn(messageBody);
	}
	
	@Test
	public void testRunWithException() throws Exception {
		
		doThrow(IllegalArgumentException.class).when(mockManager).processMessage(any(), any());
		
		// Call under test
		worker.run(mockCallback, mockMessage, mockNotification);

		verify(mockManager).processMessage(mockNotification, messageBody);
		verify(mockLogger).logWorkerFailure(eq(SESNotificationWorker.class.getName()), any(IllegalArgumentException.class), eq(false));
		verifyZeroInteractions(mockManager);
	}

	@Test
	public void testRun() throws Exception {

		// Call under test
		worker.run(mockCallback, mockMessage, mockNotification);

		verify(mockManager).processMessage(mockNotification, messageBody);
		verifyZeroInteractions(mockLogger);

	}

}
