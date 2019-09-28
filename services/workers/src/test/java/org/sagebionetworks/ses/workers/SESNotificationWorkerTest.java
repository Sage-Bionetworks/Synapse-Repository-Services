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
		doThrow(IllegalArgumentException.class).when(mockManager).processNotification(notificationBody);
		
		// Call under test
		worker.run(null, mockMessage);

		verify(mockLogger).logWorkerFailure(eq(SESNotificationWorker.class.getName()), any(IllegalArgumentException.class), eq(false));
		verifyZeroInteractions(mockManager);
	}

	@Test
	public void testRun() throws Exception {

		// @formatter:off

		String notificationBody = "{ \r\n" + 
				"  \"notificationType\":\"Bounce\",\r\n" + 
				"  \"mail\":{ \r\n" + 
				"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
				"    \"messageId\":\"000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000\",\r\n" + 
				"    \"source\":\"sender@example.com\",\r\n" + 
				"    \"destination\":[ \r\n" + 
				"      \"recipient@example.com\"\r\n" + 
				"    ],\r\n" + 
				"    \"headersTruncated\":true\r\n" + 
				"  },\r\n" + 
				"  \"bounce\":{ \r\n" + 
				"    \"bounceType\":\"Permanent\",\r\n" + 
				"    \"bounceSubType\":\"General\",\r\n" + 
				"    \"bouncedRecipients\":[ \r\n" + 
				"      { \r\n" + 
				"        \"status\":\"5.0.0\",\r\n" + 
				"        \"action\":\"failed\",\r\n" + 
				"        \"diagnosticCode\":\"smtp; 550 user unknown\",\r\n" + 
				"        \"emailAddress\":\"recipient1@example.com\"\r\n" + 
				"      },\r\n" + 
				"      { \r\n" + 
				"        \"status\":\"4.0.0\",\r\n" + 
				"        \"action\":\"delayed\",\r\n" + 
				"        \"emailAddress\":\"recipient2@example.com\"\r\n" + 
				"      }\r\n" + 
				"    ],\r\n" + 
				"    \"timestamp\":\"2012-05-25T14:59:38.605Z\",\r\n" + 
				"    \"feedbackId\":\"000001378603176d-5a4b5ad9-6f30-4198-a8c3-b1eb0c270a1d-000000\"\r\n" + 
				"  }\r\n" + 
				"}";
		
		// @formatter:on

		when(mockMessage.getBody()).thenReturn(notificationBody);
		
		// Call under test
		worker.run(null, mockMessage);

		verify(mockManager).processNotification(notificationBody);

	}

}
