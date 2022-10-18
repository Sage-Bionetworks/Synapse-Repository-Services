package org.sagebionetworks.asynchronous.workers.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@ExtendWith(MockitoExtension.class)
public class ConcurrentManagerImplTest {

	@Mock
	private CountingSemaphore mockCountingSemaphore;
	@Mock
	private AmazonSQSClient mockAmazonSQSClient;
	@Mock
	private StackStatusDao mockStackStatusDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private Runnable mockRunner;
	@Mock
	private MessageDrivenRunner mockWorker;
	@Mock
	private Message mockMessage;
	@Mock
	private ProgressListener mockProgressListener;

	@Spy
	@InjectMocks
	ConcurrentManagerImpl manager;

	private String lockKey;
	private int lockTimeoutSec;
	private int maxLockCount;
	private String queueUrl;
	private int maxThreadCount;

	@BeforeEach
	public void before() {
		lockKey = "someLockKey";
		lockTimeoutSec = 30;
		maxLockCount = 3;
		queueUrl = "https://aws-some-queue";
		maxThreadCount = 5;
	}

	@Test
	public void testIsStackAvailableForWriteWithTrue() {
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		// call under test
		assertTrue(manager.isStackAvailableForWrite());
		verify(mockStackStatusDao).isStackReadWrite();
	}

	@Test
	public void testIsStackAvailableForWriteWithFalse() {
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false);
		// call under test
		assertFalse(manager.isStackAvailableForWrite());
		verify(mockStackStatusDao).isStackReadWrite();
	}

	@Test
	public void testGetCurrentTimeMS() {
		long now = System.currentTimeMillis();
		// call under test
		assertTrue(manager.getCurrentTimeMS() >= now);
	}

	@Test
	public void testSleep() throws InterruptedException {
		long start = System.currentTimeMillis();
		// call under test
		manager.sleep(100L);
		long end = System.currentTimeMillis();
		assertTrue((end - start) >= 100);
	}

	@Test
	public void testGetSqsQueueUrl() {
		String queueName = "some-queue";
		String queueUrl = "https://aws-" + queueName;
		when(mockAmazonSQSClient.getQueueUrl(any(String.class)))
				.thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		// call under test
		String resultUrl = manager.getSqsQueueUrl(queueName);
		assertEquals(queueUrl, resultUrl);
		verify(mockAmazonSQSClient).getQueueUrl(queueName);
	}

	@Test
	public void testGetSqsQueueUrlWithNullName() {
		String queueName = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getSqsQueueUrl(queueName);
		}).getMessage();
		assertEquals("queueName is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLock() {
		String token = "a-token";
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt())).thenReturn(token);
		doAnswer((InvocationOnMock i) -> {
			((ProgressListener) i.getArgument(0)).progressMade();
			return null;
		}).when(mockCallback).addProgressListener(any());

		// call under test
		manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount);
		verify(mockCallback).addProgressListener(any());
		verify(mockCountingSemaphore).refreshLockTimeout(lockKey, token, lockTimeoutSec);
		verify(mockRunner).run();
		verify(mockCountingSemaphore).releaseLock(lockKey, token);
	}
	
	@Test
	public void testRunWithSemaphoreLockWithNullLock() {
		String token = null;
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt())).thenReturn(token);
		// call under test
		manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount);
		verifyNoMoreInteractions(mockCountingSemaphore);
	}

	@Test
	public void testRunWithSemaphoreLockWithRunException() {
		String token = "a-token";
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt())).thenReturn(token);
		doAnswer((InvocationOnMock i) -> {
			((ProgressListener) i.getArgument(0)).progressMade();
			return null;
		}).when(mockCallback).addProgressListener(any());
		doThrow(new IllegalArgumentException("nope")).when(mockRunner).run();

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		});

		verify(mockCountingSemaphore).attemptToAcquireLock(lockKey, lockTimeoutSec, maxLockCount);
		verify(mockCountingSemaphore).refreshLockTimeout(lockKey, token, lockTimeoutSec);
		verify(mockRunner).run();
		verify(mockCountingSemaphore).releaseLock(lockKey, token);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullLockKey() {
		lockKey = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("lockKey is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("callback is required.", message);
	}

	@Test
	public void testRunWithSemaphoreLockWitnNullRunner() {
		mockRunner = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.runWithSemaphoreLock(lockKey, lockTimeoutSec, maxLockCount, mockCallback, mockRunner);
		}).getMessage();
		assertEquals("runner is required.", message);
	}

	@Test
	public void testStartWorkerJobWithProgressMade() throws RecoverableMessageException, Exception {
		String receiptHandle = "receiptHandle";
		when(mockMessage.getReceiptHandle()).thenReturn(receiptHandle);
		
		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			return null;
		}).when(mockWorker).run(any(), any());

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, mockMessage);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		
		waitForFuture(job.getFuture());
		
		// the listener should be removed after the job is finished so this should be a no-op.
		job.getListener().progressMade();

		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(any());
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(new ChangeMessageVisibilityRequest()
				.withQueueUrl(queueUrl).withReceiptHandle(receiptHandle).withVisibilityTimeout(lockTimeoutSec));

		verify(mockWorker).run((ProgressCallback) job.getListener(), mockMessage);
		verify(mockAmazonSQSClient)
				.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
	}

	@Test
	public void testStartWorkerJobWithRecoverableException() throws RecoverableMessageException, Exception {
		String receiptHandle = "receiptHandle";
		when(mockMessage.getReceiptHandle()).thenReturn(receiptHandle);

		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			throw new RecoverableMessageException("Try again later");
		}).when(mockWorker).run(any(), any());

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, mockMessage);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		waitForFuture(job.getFuture());

		verify(mockAmazonSQSClient, times(2)).changeMessageVisibility(any());
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(new ChangeMessageVisibilityRequest()
				.withQueueUrl(queueUrl).withReceiptHandle(receiptHandle).withVisibilityTimeout(lockTimeoutSec));
		// second call to put the message back in the queue in 5 seconds.
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(new ChangeMessageVisibilityRequest()
				.withQueueUrl(queueUrl).withReceiptHandle(receiptHandle).withVisibilityTimeout(2));

		assertNotNull(job.getFuture());
		assertTrue(job.getFuture().isDone());
		// should not throw anything.
		job.getFuture().get();

		verify(mockWorker).run((ProgressCallback) job.getListener(), mockMessage);
		verify(mockAmazonSQSClient, never()).deleteMessage(any());
	}

	/**
	 * Helper to wait for a future to finish.
	 * 
	 * @param future
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void waitForFuture(Future<Void> future) throws InterruptedException, ExecutionException {
		assertNotNull(future);
		while (!future.isDone()) {
			Thread.sleep(100);
		}
		// will throw an exception if the worker throws an exception.
		future.get();
	}

	@Test
	public void testStartWorkerJobWithOtherException() throws RecoverableMessageException, Exception {
		String receiptHandle = "receiptHandle";
		when(mockMessage.getReceiptHandle()).thenReturn(receiptHandle);

		IllegalArgumentException toThrow = new IllegalArgumentException("Nope.");
		doThrow(toThrow).when(mockWorker).run(any(), any());

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, mockMessage);
		assertNotNull(job);
		assertNotNull(job.getListener());
		Throwable cause = assertThrows(ExecutionException.class, () -> {
			waitForFuture(job.getFuture());
		}).getCause();
		assertEquals(toThrow, cause);

		verify(mockAmazonSQSClient, never()).changeMessageVisibility(any());
		verify(mockAmazonSQSClient)
				.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNoMessages() {
		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(new ReceiveMessageResult().withMessages(Collections.emptyList()));

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker);
		assertEquals(Collections.emptyList(), jobs);

		verify(mockAmazonSQSClient).receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueUrl)
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(maxThreadCount).withVisibilityTimeout(lockTimeoutSec));

		verify(manager, never()).startWorkerJob(any(), anyInt(), any(), any());
	}

	@Test
	public void testPollForMessagesAndStartJobsWithMessages() {

		List<Message> messages = List.of(new Message().withReceiptHandle("one"),
				new Message().withReceiptHandle("two"));

		when(mockAmazonSQSClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(new ReceiveMessageResult().withMessages(messages));

		// call under test
		List<WorkerJob> jobs = manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec,
				mockWorker);

		assertNotNull(jobs);
		assertEquals(2, jobs.size());

		verify(mockAmazonSQSClient).receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueUrl)
				.withWaitTimeSeconds(0).withMaxNumberOfMessages(maxThreadCount).withVisibilityTimeout(lockTimeoutSec));

		verify(manager, times(2)).startWorkerJob(any(), anyInt(), any(), any());
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messages.get(0));
		verify(manager).startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, messages.get(1));
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNullUrl() {
		queueUrl = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker);
		}).getMessage();
		assertEquals("queueUrl is required.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithNullWorker() {
		mockWorker = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker);
		}).getMessage();
		assertEquals("worker is required.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithCountLessThanOne() {
		maxThreadCount = 0;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker);
		}).getMessage();
		assertEquals("maxNumberOfMessages must be greater than or equals to 1.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithCountMoreThanTen() {
		maxThreadCount = 11;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker);
		}).getMessage();
		assertEquals("maxNumberOfMessages must be less than or equals to 10.", message);
	}

	@Test
	public void testPollForMessagesAndStartJobsWithVisibilityLessThan10() {
		lockTimeoutSec = 9;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.pollForMessagesAndStartJobs(queueUrl, maxThreadCount, lockTimeoutSec, mockWorker);
		}).getMessage();
		assertEquals("messageVisibilityTimeoutSec must be greater than or equals to 10.", message);
	}
	
	@Test
	public void testStartWorkerJobWithShutDown() throws RecoverableMessageException, Exception {
		String receiptHandle = "receiptHandle";
		when(mockMessage.getReceiptHandle()).thenReturn(receiptHandle);
		
		doAnswer((a)->{
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			return null;
		}).when(mockWorker).run(any(), any());
		
		manager.forceShutdown();

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, mockMessage);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();
		
		waitForFuture(job.getFuture());
		
		// the listener should be removed after the job is finished so this should be a no-op.
		job.getListener().progressMade();

		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(any());
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(new ChangeMessageVisibilityRequest()
				.withQueueUrl(queueUrl).withReceiptHandle(receiptHandle).withVisibilityTimeout(lockTimeoutSec));

		verify(mockWorker).run((ProgressCallback) job.getListener(), mockMessage);
		// the message should not be deleted after shutdown.
		verify(mockAmazonSQSClient, never()).deleteMessage(any());
	}
	
	@Test
	public void testStartWorkerJobWitDoubleException() throws RecoverableMessageException, Exception {
		String receiptHandle = "receiptHandle";
		when(mockMessage.getReceiptHandle()).thenReturn(receiptHandle);
		IllegalArgumentException firstException = new IllegalArgumentException("one");

		doAnswer((a) -> {
			// We sleep to get a chance to call progressMade() before the job terminates.
			Thread.sleep(100);
			throw firstException;
		}).when(mockWorker).run(any(), any());

		when(mockAmazonSQSClient.deleteMessage(any())).thenThrow(new IllegalArgumentException("two"));

		// call under test
		WorkerJob job = manager.startWorkerJob(queueUrl, lockTimeoutSec, mockWorker, mockMessage);
		assertNotNull(job);
		assertNotNull(job.getListener());
		// progress made should refresh the lock
		job.getListener().progressMade();

		ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
			waitForFuture(job.getFuture());
		});
		assertEquals(firstException, thrown.getCause());

		// the listener should be removed after the job is finished so this should be a
		// no-op.
		job.getListener().progressMade();

		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(any());
		verify(mockAmazonSQSClient, times(1)).changeMessageVisibility(new ChangeMessageVisibilityRequest()
				.withQueueUrl(queueUrl).withReceiptHandle(receiptHandle).withVisibilityTimeout(lockTimeoutSec));

		verify(mockWorker).run((ProgressCallback) job.getListener(), mockMessage);
		verify(mockAmazonSQSClient)
				.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(receiptHandle));
	}
}
