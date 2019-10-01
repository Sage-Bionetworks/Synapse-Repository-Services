package org.sagebionetworks.ses.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.ses.SESNotificationManager;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class SESNotificationWorkerTest {

	@Mock
	private WorkerLogger mockLogger;

	@Mock
	private SESNotificationManager mockManager;

	@Mock
	private Message mockMessage;

	@InjectMocks
	private SESNotificationWorker worker;
	
	@Test
	public void testRunWithException() throws Exception {
		
		String notificationBody = "Some wrong body";
		
		when(mockMessage.getBody()).thenReturn(notificationBody);
		doThrow(IllegalArgumentException.class).when(mockManager).processMessage(notificationBody);
		
		// Call under test
		worker.run(null, mockMessage);

		verify(mockMessage).getBody();
		verify(mockLogger).logWorkerFailure(eq(SESNotificationWorker.class.getName()), any(IllegalArgumentException.class), eq(false));
		verifyZeroInteractions(mockManager);
	}

	@Test
	public void testRun() throws Exception {

		String notificationBody = "Some notification";

		when(mockMessage.getBody()).thenReturn(notificationBody);
		
		// Call under test
		worker.run(null, mockMessage);

		verify(mockMessage).getBody();
		verify(mockManager).processMessage(notificationBody);
		verifyZeroInteractions(mockLogger);

	}

}
