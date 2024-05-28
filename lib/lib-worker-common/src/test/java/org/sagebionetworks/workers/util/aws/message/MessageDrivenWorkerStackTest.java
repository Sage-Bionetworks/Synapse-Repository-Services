package org.sagebionetworks.workers.util.aws.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockKeyNotFoundException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.Gate;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@ExtendWith(MockitoExtension.class)
public class MessageDrivenWorkerStackTest {

	@Mock
	private AmazonSQSClient mockSQSClient;
	@Mock
	private CountingSemaphore mockSemaphore;
	@Mock
	private Gate mockGate;
	@Mock
	private MessageDrivenRunner mockRunner;

	private String queueUrl;
	private String token;
	private Message message;
	private MessageDrivenWorkerStackConfiguration config;

	int timeoutMS = 4000;

	private ReceiveMessageResult results;
	private ReceiveMessageResult emptyResults;

	@BeforeEach
	public void setUp() throws Exception {
		// mock queue
		queueUrl = "queueURL";

		// mock semaphore
		token = "aToken";

		// mock message receiver
		results = new ReceiveMessageResult();
		message = new Message();
		message.setReceiptHandle("handle");
		results.setMessages(Arrays.asList(message));
		emptyResults = new ReceiveMessageResult();
		emptyResults.setMessages(Collections.emptyList());

		// default config
		config = new MessageDrivenWorkerStackConfiguration();
		config.setQueueName("queueName");
		config.setGate(mockGate);
		config.setRunner(mockRunner);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(timeoutMS / 1000);
		config.setSemaphoreLockKey("lockKey");
		config.setSemaphoreMaxLockCount(2);
	}

	@Test
	public void testProgressHeartbeatEnabled() throws RecoverableMessageException, Exception {
		
		when(mockSemaphore.attemptToAcquireLock(any(String.class), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));
		when(mockGate.canRun()).thenReturn(true, true, false);
		when(mockSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);
		when(mockSQSClient.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));

		// enable heartbeat.
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(mockSemaphore, mockSQSClient, config);

		doNothing().doNothing().doNothing().doThrow(LockKeyNotFoundException.class).when(mockSemaphore)
				.refreshLockTimeout(anyString(), anyString(), anyLong());

		// setup the runner to just sleep with no progress
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// Wait for the timeout
				Thread.sleep(timeoutMS * 2);
				return null;
			}
		}).when(mockRunner).run(any(ProgressCallback.class), any(Message.class));

		// call under test
		stack.run();
		// The progress should refresh the lock timeout.
		verify(mockSQSClient, atLeast(2)).changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		verify(mockRunner).run(any(ProgressCallback.class), any(Message.class));
		verify(mockSemaphore, atLeast(3)).refreshLockTimeout("lockKey", "aToken", 4L);
	}

	@Test
	public void testProgressHeartbeatDisabled() throws RecoverableMessageException, Exception {

		when(mockSemaphore.attemptToAcquireLock(any(String.class), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));
		when(mockGate.canRun()).thenReturn(true, true, false);
		when(mockSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);
		when(mockSQSClient.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		doThrow(LockKeyNotFoundException.class).when(mockSemaphore).refreshLockTimeout(anyString(), anyString(),
				anyLong());

		// disable heartbeat.
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(mockSemaphore, mockSQSClient, config);

		// setup the runner to just sleep with no progress
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// Wait for the timeout
				Thread.sleep(timeoutMS * 2);
				return null;
			}
		}).when(mockRunner).run(any(ProgressCallback.class), any(Message.class));

		// call under test
		stack.run();
		// The progress should refresh the lock timeout.
		verify(mockRunner).run(any(ProgressCallback.class), any(Message.class));
		verify(mockSemaphore).refreshLockTimeout("lockKey", "aToken", 4L);
	}

}
