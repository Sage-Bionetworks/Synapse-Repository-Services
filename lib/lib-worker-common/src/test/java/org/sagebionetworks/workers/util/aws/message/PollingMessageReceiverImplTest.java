package org.sagebionetworks.workers.util.aws.message;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.Gate;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageSystemAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@ExtendWith(MockitoExtension.class)
public class PollingMessageReceiverImplTest {

	@Mock
	private AmazonSQSClient mockAmazonSQSClient;
	@Mock
	private MessageDrivenRunner mockRunner;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private HasQueueUrl mockHasQueueUrl;
	
	@Mock
	private Gate mockGate;
	
	PollingMessageReceiverConfiguration config;
	String queueUrl;
	int messageVisibilityTimeoutSec;
	int semaphoreLockTimeoutSec;
	ReceiveMessageResult results;
	ReceiveMessageResult emptyResults;

	Message message;

	@BeforeEach
	public void before() {
		queueUrl = "aQueueUrl";
		messageVisibilityTimeoutSec = 60;
		semaphoreLockTimeoutSec = 60;

		// setup for a single message.
		results = new ReceiveMessageResult();
		message = new Message();
		message.setReceiptHandle("handle");
		results.setMessages(Arrays.asList(message));
		emptyResults = new ReceiveMessageResult();
		emptyResults.setMessages(new LinkedList<Message>());

		config = new PollingMessageReceiverConfiguration();
		config.setHasQueueUrl(mockHasQueueUrl);
		config.setRunner(mockRunner);
		config.setMessageVisibilityTimeoutSec(messageVisibilityTimeoutSec);
		config.setSemaphoreLockTimeoutSec(semaphoreLockTimeoutSec);
		config.setGate(mockGate);

	}

	@Test
	public void testHappyConstructor() {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);

		new PollingMessageReceiverImpl(mockAmazonSQSClient, config);
	}

	@Test
	public void testNullClient() {
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(null, config);
		});
	}

	@Test
	public void testSemaphoreLockTooSmall() {
		config.setSemaphoreLockTimeoutSec(39);
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(mockAmazonSQSClient, config);
		});
	}

	@Test
	public void testSemaphoreLockLessThanVisisibleTimeout() {
		config.setSemaphoreLockTimeoutSec(config
				.getMessageVisibilityTimeoutSec() - 1);
		assertThrows(IllegalArgumentException.class, ()->{
			new PollingMessageReceiverImpl(mockAmazonSQSClient, config);
		});
	}

	@Test
	public void testRunNullMessages() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);

		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, never()).run(any(ProgressCallback.class),
				any(Message.class));
	}

	@Test
	public void testRunEmptyMessages() throws Throwable {
		ReceiveMessageResult results = new ReceiveMessageResult();
		results.setMessages(new LinkedList<Message>());
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);

		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, never()).run(any(ProgressCallback.class),
				any(Message.class));
	}

	@Test
	public void testRunTooMessages() throws Exception {
		ReceiveMessageResult results = new ReceiveMessageResult();
		Message message = new Message();
		results.setMessages(Arrays.asList(message, message));
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		
		when(mockGate.canRun()).thenReturn(true, false);
		
		assertThrows(IllegalStateException.class, ()->{
			// call under test
			receiver.run(mockProgressCallback);
		});
	}

	@Test
	public void testOneMessage() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		
		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, times(1)).run(any(ProgressCallback.class),
				any(Message.class));
		// The message should be deleted
		DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
		deleteMessageRequest.setQueueUrl(queueUrl);
		deleteMessageRequest.setReceiptHandle(message.getReceiptHandle());
		verify(mockAmazonSQSClient, times(1)).deleteMessage(
				deleteMessageRequest);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testMessageDeleteOnException()
			throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		// setup the runner to throw an unknown exception
		doThrow(new IllegalArgumentException("Something was null")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		
		when(mockGate.canRun()).thenReturn(true, false);
		
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			receiver.run(mockProgressCallback);
		});
		// The message should be deleted for any non-RecoverableMessageException
		DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
		deleteMessageRequest.setQueueUrl(queueUrl);
		deleteMessageRequest.setReceiptHandle(message.getReceiptHandle());
		verify(mockAmazonSQSClient, times(1)).deleteMessage(
				deleteMessageRequest);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testMessageNoDeleteRecoverableMessageException()
			throws Throwable {
		
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		
		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		// The message should not be deleted for RecoverableMessageException
		verify(mockAmazonSQSClient, never()).deleteMessage(
				any(DeleteMessageRequest.class));
		// The visibility of the message should be reset
		ChangeMessageVisibilityRequest expectedRequest = new ChangeMessageVisibilityRequest(
				this.queueUrl, this.message.getReceiptHandle(),
				PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC);
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(
				expectedRequest);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}
	
	@Test
	public void testMessageWithRecoverableMessageException() throws Throwable {
		
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(mockAmazonSQSClient, config);

		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class)))
			.thenReturn(new ReceiveMessageResult().withMessages(Arrays.asList(message)));
		
		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(mockRunner).run(any(ProgressCallback.class), any(Message.class));
		
		int count = PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC + 2;
		
		for (int retryNumber = 1; retryNumber <= count; retryNumber++) {
			message.setAttributes(
				Collections.singletonMap(MessageSystemAttributeName.ApproximateReceiveCount.toString(), String.valueOf(retryNumber))
			);
			
			when(mockGate.canRun()).thenReturn(true, false);
			
			// call under test
			receiver.run(mockProgressCallback);
			
			int expectedTimeout = Math.min(retryNumber, PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC);
			
			// The visibility of the message should be reset
			ChangeMessageVisibilityRequest expectedRequest = new ChangeMessageVisibilityRequest(this.queueUrl, this.message.getReceiptHandle(), expectedTimeout);
			
			if (retryNumber <= PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC) {
				verify(mockAmazonSQSClient).changeMessageVisibility(expectedRequest);
			} else {
				verify(mockAmazonSQSClient, times(retryNumber - PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC + 1)).changeMessageVisibility(expectedRequest);
			}
		}
		
		verify(mockProgressCallback, times(count)).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback, times(count)).removeProgressListener(any(ProgressListener.class));
		
		// The message should not be deleted for RecoverableMessageException
		verify(mockAmazonSQSClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	public void testMessageWithRecoverableMessageExceptionAndRetryCountExceedMax()
			throws Throwable {
		
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		
		Integer retryCount = 10;
		
		message.setAttributes(
			Collections.singletonMap(MessageSystemAttributeName.ApproximateReceiveCount.toString(), retryCount.toString())
		);
		
		// setup the runner to throw a RecoverableMessageException
		doThrow(new RecoverableMessageException("Try again later.")).when(
				mockRunner)
				.run(any(ProgressCallback.class), any(Message.class));

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		
		when(mockGate.canRun()).thenReturn(true, false);

		// call under test
		receiver.run(mockProgressCallback);
		// The message should not be deleted for RecoverableMessageException
		verify(mockAmazonSQSClient, never()).deleteMessage(
				any(DeleteMessageRequest.class));
		// The visibility of the message should be reset
		ChangeMessageVisibilityRequest expectedRequest = new ChangeMessageVisibilityRequest(this.queueUrl, this.message.getReceiptHandle(), 
				PollingMessageReceiverImpl.RETRY_MAX_MESSAGE_VISIBILITY_TIMEOUT_SEC);
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(
				expectedRequest);
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}

	
	@Test
	public void testRunnerShouldTerminate() throws Exception {
		
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		
		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);

		//always return a message
		ReceiveMessageResult results = new ReceiveMessageResult();
		Message message = new Message();
		results.setMessages(Collections.singletonList(message));
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer((invocationOnMock) -> results);

		//should not terminate for 3 times that we check.
		when(mockGate.canRun()).thenReturn(true, true,true,true,false);

		// call under test
		receiver.run(mockProgressCallback);

		ReceiveMessageRequest expectedRequest = new ReceiveMessageRequest()
			.withAttributeNames(Arrays.asList(MessageSystemAttributeName.ApproximateReceiveCount.toString()))
			.withMaxNumberOfMessages(1)
			.withVisibilityTimeout(messageVisibilityTimeoutSec)
			.withWaitTimeSeconds(0)
			.withQueueUrl(queueUrl);
		
		//since receiver.run() uses a do/while loop. we've done work for numInvocations("progressCallback.runnerShouldTerminate()") + 1 times
		verify(mockAmazonSQSClient, times(4)).receiveMessage(expectedRequest);
		verify(mockRunner, times(4)).run(any(ProgressCallback.class), any(Message.class));
		verify(mockAmazonSQSClient, times(4)).deleteMessage(any(DeleteMessageRequest.class));
		verify(mockProgressCallback, times(4)).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback, times(4)).removeProgressListener(any(ProgressListener.class));
	}
	
	@Test
	public void testDeleteOnShutdown() throws Throwable {
		when(mockHasQueueUrl.getQueueUrl()).thenReturn(queueUrl);
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(results, emptyResults);

		PollingMessageReceiverImpl receiver = new PollingMessageReceiverImpl(
				mockAmazonSQSClient, config);
		
		when(mockGate.canRun()).thenReturn(true, false);

		// Simulate a JVM shutdown.
		receiver.forceShutdown();
		// call under test
		receiver.run(mockProgressCallback);
		verify(mockRunner, times(1)).run(any(ProgressCallback.class),
				any(Message.class));
		// The message should not be deleted after a shutdown.
		verify(mockAmazonSQSClient, never()).deleteMessage(any());
		verify(mockProgressCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProgressCallback).removeProgressListener(any(ProgressListener.class));
	}
	
}
