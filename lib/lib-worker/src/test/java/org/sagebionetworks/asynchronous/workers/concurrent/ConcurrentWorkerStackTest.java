package org.sagebionetworks.asynchronous.workers.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack.StackState;
import org.sagebionetworks.util.progress.ProgressListener;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;

@ExtendWith(MockitoExtension.class)
public class ConcurrentWorkerStackTest {

	@Mock
	private ConcurrentManager mockManager;
	@Mock
	private MessageDrivenRunner mockWorker;
	@Mock
	private ProgressListener mockProgressListenerOne;
	@Mock
	private ProgressListener mockProgressListenerTwo;
	@Mock
	private ProgressListener mockProgressListenerThree;
	@Mock
	private ProgressListener mockProgressListenerFour;
	@Mock
	private Future<Void> futureOne;
	@Mock
	private Future<Void> futureTwo;
	@Mock
	private Future<Void> futureThree;
	@Mock
	private Future<Void> futureFour;

	private boolean canRunInReadOnly;
	private String semaphoreLockKey;
	private Integer semaphoreMaxLockCount;
	private Integer semaphoreLockAndMessageVisibilityTimeoutSec;
	private Integer maxThreadsPerMachine;
	private String queueName;
	private String queueUrl;

	@BeforeEach
	public void before() {
		canRunInReadOnly = false;
		semaphoreLockKey = "semaphoreKey";
		semaphoreMaxLockCount = 10;
		semaphoreLockAndMessageVisibilityTimeoutSec = 30;
		maxThreadsPerMachine = 8;
		queueName = "queue-name";
		queueUrl = "https://aws-some-queue";
	}

	ConcurrentWorkerStack createStack() {
		return ConcurrentWorkerStack.builder().withSingleton(mockManager).withCanRunInReadOnly(canRunInReadOnly)
				.withSemaphoreLockKey(semaphoreLockKey).withSemaphoreMaxLockCount(semaphoreMaxLockCount)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(semaphoreLockAndMessageVisibilityTimeoutSec)
				.withMaxThreadsPerMachine(maxThreadsPerMachine).withWorker(mockWorker).withQueueName(queueName).build();
	}

	@Test
	public void testBuildWithNullSingleton() {
		mockManager = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();

		}).getMessage();
		assertEquals("manager is required.", message);
	}

	@Test
	public void testBuildWithNullWorker() {
		mockWorker = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("worker is required.", message);
	}

	@Test
	public void testBuildWithNullQueueName() {
		queueName = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("queueName is required.", message);
	}

	@Test
	public void testBuildWithNullKey() {
		semaphoreLockKey = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("semaphoreLockKey is required.", message);
	}

	@Test
	public void testBuildWithNullLockCount() {
		semaphoreMaxLockCount = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("semaphoreMaxLockCount is required.", message);
	}

	@Test
	public void testBuildWithNullVisibility() {
		semaphoreLockAndMessageVisibilityTimeoutSec = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("semaphoreLockAndMessageVisibilityTimeoutSec is required.", message);
	}

	@Test
	public void testBuildWithNullMaxThreads() {
		maxThreadsPerMachine = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();

		}).getMessage();
		assertEquals("maxThreadsPerMachine is required.", message);
	}

	@Test
	public void testBuildWithVisiblityLess30() {
		semaphoreLockAndMessageVisibilityTimeoutSec = 29;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("semaphoreLockAndMessageVisibilityTimeoutSec must be greater than or equal to 30 seconds.",
				message);
	}

	@Test
	public void testBuildWithLockCountLessThanOne() {
		semaphoreMaxLockCount = 0;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("semaphoreMaxLockCount must be greater than or equals to 1.", message);
	}

	@Test
	public void testBuildWithMaxThreadLessThanOne() {
		maxThreadsPerMachine = 0;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			createStack();
		}).getMessage();
		assertEquals("maxThreadsPerMachine must be greater than or equal to 1.", message);
	}

	@Test
	public void testRunWithCannotProcessMoreMessages() throws InterruptedException {
		ConcurrentWorkerStack stack = Mockito.spy(createStack());

		doReturn(false).when(stack).canProcessMoreMessages();

		// call under test
		stack.run();

		verify(stack).canProcessMoreMessages();
		verify(mockManager, never()).runWithSemaphoreLock(any(), anyInt(), anyInt(), any(), any());
	}

	@Test
	public void testRunWithInterruptSleepAndNoWorkerAdded() throws InterruptedException {
		canRunInReadOnly = true;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		doAnswer((InvocationOnMock i) -> {
			((Runnable) i.getArgument(4)).run();
			return null;
		}).when(mockManager).runWithSemaphoreLock(any(), anyInt(), anyInt(), any(), any());

		doNothing().doNothing().doNothing().doNothing().doThrow(new InterruptedException()).when(mockManager)
				.sleep(anyLong());

		ConcurrentWorkerStack stack = Mockito.spy(createStack());

		doReturn(true).when(stack).canProcessMoreMessages();
		doNothing().when(stack).resetNextRefreshTimeMS();
		doNothing().when(stack).checkRunningJobs();
		doReturn(false).when(stack).attemptToAddMoreWorkers();

		// call under test
		stack.run();

		verify(stack).resetAllState();
		verify(stack).canProcessMoreMessages();
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager).runWithSemaphoreLock(eq(semaphoreLockKey),
				eq(semaphoreLockAndMessageVisibilityTimeoutSec), eq(semaphoreMaxLockCount), any(), any());
		verify(mockManager, times(5)).sleep(ConcurrentWorkerStack.MAX_WAIT_TIME);
		verify(stack, times(5)).refreshLocksIfNeeded();
		verify(stack, times(5)).checkRunningJobs();
		verify(stack, times(5)).attemptToAddMoreWorkers();
		verify(stack).startShutdown();
	}
	
	@Test
	public void testRunWithInterruptSleepAndNewWorkersAdded() throws InterruptedException {
		canRunInReadOnly = true;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		doAnswer((InvocationOnMock i) -> {
			((Runnable) i.getArgument(4)).run();
			return null;
		}).when(mockManager).runWithSemaphoreLock(any(), anyInt(), anyInt(), any(), any());

		doNothing().doNothing().doNothing().doNothing().doThrow(new InterruptedException()).when(mockManager)
				.sleep(anyLong());

		ConcurrentWorkerStack stack = Mockito.spy(createStack());

		doReturn(true).when(stack).canProcessMoreMessages();
		doNothing().when(stack).resetNextRefreshTimeMS();
		doNothing().when(stack).checkRunningJobs();
		doReturn(true).when(stack).attemptToAddMoreWorkers();

		// call under test
		stack.run();

		verify(stack).resetAllState();
		verify(stack).canProcessMoreMessages();
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager).runWithSemaphoreLock(eq(semaphoreLockKey),
				eq(semaphoreLockAndMessageVisibilityTimeoutSec), eq(semaphoreMaxLockCount), any(), any());
		verify(mockManager, times(5)).sleep(ConcurrentWorkerStack.MIN_WAIT_TIME);
		verify(stack, times(5)).refreshLocksIfNeeded();
		verify(stack, times(5)).checkRunningJobs();
		verify(stack, times(5)).attemptToAddMoreWorkers();
		verify(stack).startShutdown();
	}

	@Test
	public void testCanProcessMoreMessagesWithNoRunInReadOnlyAndInReadOnly() throws InterruptedException {

		canRunInReadOnly = false;
		when(mockManager.isStackAvailableForWrite()).thenReturn(false);
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		ConcurrentWorkerStack stack = createStack();

		// call under test
		assertFalse(stack.canProcessMoreMessages());
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager).isStackAvailableForWrite();
	}

	@Test
	public void testCanProcessMoreMessagesWithNoRunInReadOnlyAndInReadWrite() throws InterruptedException {
		canRunInReadOnly = false;
		when(mockManager.isStackAvailableForWrite()).thenReturn(true);
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		ConcurrentWorkerStack stack = createStack();

		// call under test
		assertTrue(stack.canProcessMoreMessages());
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager).isStackAvailableForWrite();
	}

	@Test
	public void testCanProcessMoreMessagesWithCanunInReadOnly() throws InterruptedException {

		canRunInReadOnly = true;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		ConcurrentWorkerStack stack = createStack();

		// call under test
		assertTrue(stack.canProcessMoreMessages());
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager, never()).isStackAvailableForWrite();
	}

	@Test
	public void testCanProcessMoreMessagesWithAfterShutdown() throws InterruptedException {
		canRunInReadOnly = false;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		ConcurrentWorkerStack stack = createStack();

		stack.startShutdown();
		// call under test
		assertFalse(stack.canProcessMoreMessages());
		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager, never()).isStackAvailableForWrite();
	}

	@Test
	public void testResetNextRefreshTimeMS() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		ConcurrentWorkerStack stack = createStack();

		when(mockManager.getCurrentTimeMS()).thenReturn(101L);

		assertEquals((semaphoreLockAndMessageVisibilityTimeoutSec * 1000) / 3, stack.getLockRefreshFrequencyMS());

		// call under test
		stack.resetNextRefreshTimeMS();

		assertEquals(10000L + 101L, stack.getNextRefreshTimeMS());
		verify(mockManager).getCurrentTimeMS();
	}

	@Test
	public void testResetAllState() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		when(mockManager.getCurrentTimeMS()).thenReturn(1L, 2L, 3L);

		ConcurrentWorkerStack stack = createStack();

		stack.resetAllState();
		stack.startShutdown();
		assertEquals(StackState.SHUT_DOWN, stack.getState());
		ConcurrentProgressCallback oldCallback = stack.getLockCallback();
		List<WorkerJob> oldJobs = stack.getRunningJobs();
		assertEquals(10000L + 1L, stack.getNextRefreshTimeMS());

		// call under test
		stack.resetAllState();

		assertEquals(StackState.CONTINUE, stack.getState());
		assertFalse(oldJobs == stack.getRunningJobs());
		assertFalse(oldCallback == stack.getLockCallback());
		assertEquals(10000L + 2L, stack.getNextRefreshTimeMS());

		verify(mockManager).getSqsQueueUrl(queueName);
		verify(mockManager, times(2)).getCurrentTimeMS();
	}

	@Test
	public void testShouldContinueRunningWithRunningJobAndStateContinue() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		when(mockManager.getCurrentTimeMS()).thenReturn(1L, 2L, 3L);

		ConcurrentWorkerStack stack = createStack();
		stack.resetAllState();
		stack.getRunningJobs().add(new WorkerJob(futureOne, mockProgressListenerOne));

		// call under test
		assertTrue(stack.shouldContinueRunning());
	}

	@Test
	public void testShouldContinueRunningWithRunningJobAndStateShutdown() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		when(mockManager.getCurrentTimeMS()).thenReturn(1L, 2L, 3L);

		ConcurrentWorkerStack stack = createStack();
		stack.resetAllState();
		stack.getRunningJobs().add(new WorkerJob(futureOne, mockProgressListenerOne));
		stack.startShutdown();

		// call under test
		assertTrue(stack.shouldContinueRunning());
	}

	@Test
	public void testShouldContinueRunningWithNoRunningJobAndStateShutdown() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		when(mockManager.getCurrentTimeMS()).thenReturn(1L, 2L, 3L);

		ConcurrentWorkerStack stack = createStack();
		stack.resetAllState();
		stack.getRunningJobs().clear();
		stack.startShutdown();

		// call under test
		assertFalse(stack.shouldContinueRunning());
	}

	@Test
	public void testRefreshLocksIfNeeded() {
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);

		when(mockManager.getCurrentTimeMS()).thenReturn(1L, 2L, 3L);

		ConcurrentWorkerStack stack = createStack();
		stack.resetAllState();
		// two listeners for workers
		stack.getRunningJobs().add(new WorkerJob(futureOne, mockProgressListenerOne));
		stack.getRunningJobs().add(new WorkerJob(futureTwo, mockProgressListenerTwo));
		// one listener for the semaphore lock
		stack.getLockCallback().addProgressListener(mockProgressListenerThree);

		stack.resetNextRefreshTimeMS();
		assertEquals(10_002L, stack.getNextRefreshTimeMS());

		// call under test
		stack.refreshLocksIfNeeded();
		verify(mockProgressListenerOne, never()).progressMade();
		verify(mockProgressListenerTwo, never()).progressMade();
		verify(mockProgressListenerThree, never()).progressMade();
		verify(mockManager, times(3)).getCurrentTimeMS();
		assertEquals(10_002L, stack.getNextRefreshTimeMS());

		reset(mockProgressListenerOne, mockProgressListenerTwo, mockProgressListenerThree, mockManager);
		when(mockManager.getCurrentTimeMS()).thenReturn(10_002L, 10_003L);

		// call under test
		stack.refreshLocksIfNeeded();
		verify(mockProgressListenerOne).progressMade();
		verify(mockProgressListenerTwo).progressMade();
		verify(mockProgressListenerThree).progressMade();
		verify(mockManager, times(2)).getCurrentTimeMS();
		assertEquals(20_003L, stack.getNextRefreshTimeMS());

		reset(mockProgressListenerOne, mockProgressListenerTwo, mockProgressListenerThree, mockManager);
		when(mockManager.getCurrentTimeMS()).thenReturn(10_004L);

		// call under test
		stack.refreshLocksIfNeeded();
		verify(mockProgressListenerOne, never()).progressMade();
		verify(mockProgressListenerTwo, never()).progressMade();
		verify(mockProgressListenerThree, never()).progressMade();
		verify(mockManager, times(1)).getCurrentTimeMS();
		assertEquals(20_003L, stack.getNextRefreshTimeMS());

		reset(mockProgressListenerOne, mockProgressListenerTwo, mockProgressListenerThree, mockManager);
		when(mockManager.getCurrentTimeMS()).thenReturn(20_008L, 20_009L);

		// call under test
		stack.refreshLocksIfNeeded();
		verify(mockProgressListenerOne).progressMade();
		verify(mockProgressListenerTwo).progressMade();
		verify(mockProgressListenerThree).progressMade();
		verify(mockManager, times(2)).getCurrentTimeMS();
		assertEquals(30_009L, stack.getNextRefreshTimeMS());
	}

	@Test
	public void testCheckRunningJobs() throws InterruptedException, ExecutionException {
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();
		List<WorkerJob> jobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree));
		stack.getRunningJobs().addAll(jobs);

		when(futureOne.isDone()).thenReturn(false);
		when(futureTwo.isDone()).thenReturn(true);
		when(futureTwo.get()).thenReturn(null);
		when(futureThree.isDone()).thenReturn(false);

		assertEquals(3, stack.getRunningJobs().size());
		assertEquals(StackState.CONTINUE, stack.getState());

		// call under test
		stack.checkRunningJobs();

		// two should be removed since it was done
		List<WorkerJob> remainingJobs = List.of(jobs.get(0), jobs.get(2));
		assertEquals(remainingJobs, stack.getRunningJobs());
		assertEquals(StackState.CONTINUE, stack.getState());
		verify(stack, never()).startShutdown();

		verify(futureOne).isDone();
		verify(futureOne, never()).get();

		verify(futureTwo).isDone();
		verify(futureTwo).get();

		verify(futureThree).isDone();
		verify(futureThree, never()).get();
	}

	@Test
	public void testCheckRunningJobsWithInterruptException() throws Exception {
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();
		List<WorkerJob> jobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree));
		stack.getRunningJobs().addAll(jobs);

		when(futureOne.isDone()).thenReturn(false);
		when(futureTwo.isDone()).thenReturn(true);
		doThrow(new InterruptedException()).when(futureTwo).get();
		when(futureThree.isDone()).thenReturn(false);

		assertEquals(3, stack.getRunningJobs().size());
		assertEquals(StackState.CONTINUE, stack.getState());

		// call under test
		stack.checkRunningJobs();

		// two should be removed since it was done
		List<WorkerJob> remainingJobs = List.of(jobs.get(0), jobs.get(2));
		assertEquals(remainingJobs, stack.getRunningJobs());
		// interrupt should trigger a shutdown
		assertEquals(StackState.SHUT_DOWN, stack.getState());
		verify(stack).startShutdown();

		verify(futureOne).isDone();
		verify(futureOne, never()).get();

		verify(futureTwo).isDone();
		verify(futureTwo).get();

		verify(futureThree).isDone();
		verify(futureThree, never()).get();
	}

	@Test
	public void testCheckRunningJobsWithExecutionException() throws Exception {
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();
		List<WorkerJob> jobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree));
		stack.getRunningJobs().addAll(jobs);

		when(futureOne.isDone()).thenReturn(false);
		when(futureTwo.isDone()).thenReturn(true);
		doThrow(new ExecutionException(new IllegalArgumentException("not an interrupt"))).when(futureTwo).get();
		when(futureThree.isDone()).thenReturn(false);

		assertEquals(3, stack.getRunningJobs().size());
		assertEquals(StackState.CONTINUE, stack.getState());

		// call under test
		stack.checkRunningJobs();

		// two should be removed since it was done
		List<WorkerJob> remainingJobs = List.of(jobs.get(0), jobs.get(2));
		assertEquals(remainingJobs, stack.getRunningJobs());
		// interrupt should trigger a shutdown
		assertEquals(StackState.CONTINUE, stack.getState());
		verify(stack, never()).startShutdown();

		verify(futureOne).isDone();
		verify(futureOne, never()).get();

		verify(futureTwo).isDone();
		verify(futureTwo).get();

		verify(futureThree).isDone();
		verify(futureThree, never()).get();
	}

	@Test
	public void testAttemptToAddMoreWorkers() {
		maxThreadsPerMachine = 8;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		doReturn(true).when(stack).canProcessMoreMessages();

		List<WorkerJob> jobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree));

		when(mockManager.pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any())).thenReturn(jobs);

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertTrue(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(jobs, stack.getRunningJobs());
		verify(stack).canProcessMoreMessages();
		int maxNumberOfMessages = maxThreadsPerMachine;
		verify(mockManager).pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessages,
				semaphoreLockAndMessageVisibilityTimeoutSec, mockWorker);

	}
	
	@Test
	public void testAttemptToAddMoreWorkersWithNoAddedWorkers() {
		maxThreadsPerMachine = 8;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		doReturn(true).when(stack).canProcessMoreMessages();

		List<WorkerJob> jobs = Collections.emptyList();

		when(mockManager.pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any())).thenReturn(jobs);

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertFalse(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(jobs, stack.getRunningJobs());
		verify(stack).canProcessMoreMessages();
		int maxNumberOfMessages = maxThreadsPerMachine;
		verify(mockManager).pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessages,
				semaphoreLockAndMessageVisibilityTimeoutSec, mockWorker);

	}

	@Test
	public void testAttemptToAddMoreWorkersWithRuningJobs() {
		maxThreadsPerMachine = 4;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		List<WorkerJob> allJobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree),
				new WorkerJob(futureFour, mockProgressListenerFour));

		doReturn(true).when(stack).canProcessMoreMessages();
		// start with one running job
		stack.getRunningJobs().add(allJobs.get(0));

		// three will get added
		when(mockManager.pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any()))
				.thenReturn(List.of(allJobs.get(1), allJobs.get(2), allJobs.get(3)));

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertTrue(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(allJobs, stack.getRunningJobs());
		verify(stack).canProcessMoreMessages();
		int maxNumberOfMessages = 3;
		verify(mockManager).pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessages,
				semaphoreLockAndMessageVisibilityTimeoutSec, mockWorker);

	}

	@Test
	public void testAttemptToAddMoreWorkersWithMaxThreadEqualMaxSqsMessagesPerRequest() {
		maxThreadsPerMachine = 10;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		List<WorkerJob> allJobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree),
				new WorkerJob(futureFour, mockProgressListenerFour));

		doReturn(true).when(stack).canProcessMoreMessages();

		// three will get added
		when(mockManager.pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any())).thenReturn(allJobs);

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertTrue(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(allJobs, stack.getRunningJobs());
		verify(stack).canProcessMoreMessages();
		int maxNumberOfMessages = 10;
		verify(mockManager).pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessages,
				semaphoreLockAndMessageVisibilityTimeoutSec, mockWorker);

	}

	@Test
	public void testAttemptToAddMoreWorkersWithMaxThreadExceedsMaxSqsMessagesPerRequest() {
		maxThreadsPerMachine = 11;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		List<WorkerJob> allJobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree),
				new WorkerJob(futureFour, mockProgressListenerFour));

		doReturn(true).when(stack).canProcessMoreMessages();

		// three will get added
		when(mockManager.pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any())).thenReturn(allJobs);

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertTrue(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(allJobs, stack.getRunningJobs());
		verify(stack).canProcessMoreMessages();
		int maxNumberOfMessages = 10;
		verify(mockManager).pollForMessagesAndStartJobs(queueUrl, maxNumberOfMessages,
				semaphoreLockAndMessageVisibilityTimeoutSec, mockWorker);

	}

	@Test
	public void testAttemptToAddMoreWorkersWithNoEmptySlots() {
		maxThreadsPerMachine = 4;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		List<WorkerJob> allJobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree),
				new WorkerJob(futureFour, mockProgressListenerFour));

		stack.getRunningJobs().addAll(allJobs);

		doReturn(true).when(stack).canProcessMoreMessages();

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertFalse(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(allJobs, stack.getRunningJobs());
		verify(mockManager, never()).pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any());
	}

	@Test
	public void testAttemptToAddMoreWorkersWithCannotProcessMoreMessages() {
		maxThreadsPerMachine = 4;
		when(mockManager.getSqsQueueUrl(any())).thenReturn(queueUrl);
		ConcurrentWorkerStack stack = Mockito.spy(createStack());
		stack.resetAllState();

		List<WorkerJob> allJobs = List.of(new WorkerJob(futureOne, mockProgressListenerOne),
				new WorkerJob(futureTwo, mockProgressListenerTwo),
				new WorkerJob(futureThree, mockProgressListenerThree),
				new WorkerJob(futureFour, mockProgressListenerFour));

		stack.getRunningJobs().addAll(allJobs);

		doReturn(false).when(stack).canProcessMoreMessages();

		// call under test
		boolean result = stack.attemptToAddMoreWorkers();
		
		assertFalse(result);

		verify(mockManager).getSqsQueueUrl(queueName);
		assertEquals(allJobs, stack.getRunningJobs());
		verify(mockManager, never()).pollForMessagesAndStartJobs(any(), anyInt(), anyInt(), any());
	}
}
