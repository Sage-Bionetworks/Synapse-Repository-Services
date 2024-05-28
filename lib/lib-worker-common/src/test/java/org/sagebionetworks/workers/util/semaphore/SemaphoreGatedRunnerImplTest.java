package org.sagebionetworks.workers.util.semaphore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.sagebionetworks.workers.util.Gate;

@ExtendWith(MockitoExtension.class)
public class SemaphoreGatedRunnerImplTest {

	@Mock
	private CountingSemaphore mockSemaphore;
	private SemaphoreGatedRunnerConfiguration config;
	private SemaphoreGatedRunnerImpl semaphoreGatedRunner;
	@Mock
	private ProgressingRunner mockRunner;

	@Mock
	private Gate mockGate;
	private String lockKey;
	private long lockTimeoutSec;
	private long lockTimeoutMS;
	private int maxLockCount;
	
	private String atoken;
	
	@BeforeEach
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		lockKey = "aKey";
		lockTimeoutSec = 4;
		lockTimeoutMS = lockTimeoutSec*1000;
		maxLockCount = 2;

		config = new SemaphoreGatedRunnerConfiguration(mockRunner, lockKey, lockTimeoutSec, maxLockCount);
		semaphoreGatedRunner = new SemaphoreGatedRunnerImpl(mockSemaphore, config, mockGate);
		
		atoken = "atoken";
	}
	
	@Test
	public void testConfigureBad(){
		mockRunner = null;
		config = null;
		assertThrows(IllegalArgumentException.class, ()->{
			semaphoreGatedRunner = new SemaphoreGatedRunnerImpl(mockSemaphore, config, mockGate);
		});

	}
	
	@Test
	public void testHappy() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		when(mockGate.canRun()).thenReturn(true);
		// start the semaphoreGatedRunner
		semaphoreGatedRunner.run();
		// runner should be run
		verify(mockRunner).run(any(ProgressCallback.class));
		// The lock should get released.
		verify(mockSemaphore).releaseLock(lockKey, atoken);
		// The lock should be refreshed for this case.
		verify(mockSemaphore).refreshLockTimeout(anyString(), anyString(), anyLong());
	}
	
	@Test
	public void testLockReleaseOnException() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		when(mockGate.canRun()).thenReturn(true);
		// The lock must be released on exception.
		// Simulate an exception thrown by the runner.
		doThrow(new RuntimeException("Something went wrong!")).when(mockRunner).run(any(ProgressCallback.class));
		// Issue a lock.
		String atoken = "atoken";
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		semaphoreGatedRunner.run();
		// The lock should get released.
		verify(mockSemaphore).releaseLock(lockKey, atoken);
	}
	
	@Test
	public void testLockNotAcquired() throws Exception{
		// Empty is returned when a lock cannot be acquired.
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		when(mockGate.canRun()).thenReturn(true);
		
		// Start the run
		semaphoreGatedRunner.run();
		// the lock should not be released or refreshed.
		verify(mockSemaphore, never()).refreshLockTimeout(anyString(), anyString(), anyLong());
		verify(mockSemaphore, never()).releaseLock(anyString(), anyString());
		// The worker should not get called.
		verify(mockRunner, never()).run(any(ProgressCallback.class));
	}
	
	@Test
	public void testExceptionOnAcquireLock() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenThrow(new OutOfMemoryError("Something bad!"));
		when(mockGate.canRun()).thenReturn(true);
		
		// Start the run. The exception should not make it out of the runner.
		semaphoreGatedRunner.run();
		// the lock should not be released or refreshed.
		verify(mockSemaphore, never()).refreshLockTimeout(anyString(), anyString(), anyLong());
		verify(mockSemaphore, never()).releaseLock(anyString(), anyString());
		// The worker should not get called.
		verify(mockRunner, never()).run(any(ProgressCallback.class));
	}
	
	@Test
	public void testLockReleaseFailures() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		when(mockGate.canRun()).thenReturn(true);
		doThrow(new LockReleaseFailedException("Failed to release the lock!")).when(mockSemaphore).releaseLock(lockKey,  atoken);
		
		assertThrows(LockReleaseFailedException.class, ()->{
			// start the semaphoreGatedRunner
			semaphoreGatedRunner.run();
		});

	}
	
	@Test
	public void testProgressHeartbeat() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		when(mockGate.canRun()).thenReturn(true);
		// disable the heartbeat.
		semaphoreGatedRunner = new SemaphoreGatedRunnerImpl(mockSemaphore, config, mockGate);
		setupRunnerSleep();

		// call under test.
		semaphoreGatedRunner.run();

		//  heartbeat progress events should occur
		verify(mockSemaphore, atLeast(2)).refreshLockTimeout(anyString(), anyString(), anyLong());
	}

	@Test
	public void testRun_canRunIsTrue() throws Exception {
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		when(mockGate.canRun()).thenReturn(true);
		setupRunnerSleep();

		// call under test.
		semaphoreGatedRunner.run();

		verify(mockRunner).run(any(ProgressCallback.class));
		verify(mockSemaphore, atLeast(2)).refreshLockTimeout(anyString(), anyString(), anyLong());
	}

	@Test
	public void testRun_gateIsNull() throws Exception {
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		semaphoreGatedRunner = new SemaphoreGatedRunnerImpl(mockSemaphore, config, null);
		setupRunnerSleep();

		// call under test.
		semaphoreGatedRunner.run();

		verify(mockRunner).run(any(ProgressCallback.class));
		verify(mockSemaphore, atLeast(2)).refreshLockTimeout(anyString(), anyString(), anyLong());
	}

	@Test
	public void testRun_canRunIsFalse(){
		when(mockGate.canRun()).thenReturn(false);

		// call under test.
		semaphoreGatedRunner.run();

		verifyZeroInteractions(mockRunner);
	}

	@Test
	public void testRunWithCanRunTrueAndThenFalse() throws Exception {
		
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(atoken));
		
		when(mockGate.canRun()).thenReturn(true, false);
		setupRunnerSleep();

		// call under test.
		semaphoreGatedRunner.run();

		verify(mockRunner).run(any(ProgressCallback.class));
		// Fix for PLFM-7432: refresh should occur even when canRun() is false.
		verify(mockSemaphore, atLeast(2)).refreshLockTimeout(anyString(), anyString(), anyLong());
	}


	@Test
	public void canRun_nullGate(){
		semaphoreGatedRunner = new SemaphoreGatedRunnerImpl(mockSemaphore, config, null);
		assertTrue(semaphoreGatedRunner.canRun());
	}

	@Test
	public void canRun_GateCanRunTrue(){
		when(mockGate.canRun()).thenReturn(true);
		
		assertTrue(semaphoreGatedRunner.canRun());
	}

	@Test
	public void canRun_GateCanRunFalse(){
		when(mockGate.canRun()).thenReturn(false);
		assertFalse(semaphoreGatedRunner.canRun());
	}


	private void setupRunnerSleep() throws Exception {
		// Setup the worker to sleep without making progress.
		doAnswer(invocation -> {
			Thread.sleep(lockTimeoutMS*2);
			return null;
		}).when(mockRunner).run(any(ProgressCallback.class));
	}
}
