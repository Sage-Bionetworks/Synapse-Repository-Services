package org.sagebionetworks.workers.util.semaphore;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.sagebionetworks.workers.util.Gate;

@ExtendWith(MockitoExtension.class)
public class SemaphoreGatedWorkerStackTest {
	
	@Mock
	private CountingSemaphore mockSemaphore;
	@Mock
	private ProgressingRunner mockRunner;
	@Mock
	private Gate mockGate;
	private SemaphoreGatedWorkerStackConfiguration config;
	private String token;

	@BeforeEach
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		token = "aToken";
		
		config = new SemaphoreGatedWorkerStackConfiguration();
		config.setGate(mockGate);
		config.setProgressingRunner(mockRunner);
		config.setSemaphoreLockKey("lockKey");
		config.setSemaphoreLockTimeoutSec(10);
		config.setSemaphoreMaxLockCount(2);
	}
	
	@Test
	public void testHappyRun() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));
		when(mockGate.canRun()).thenReturn(true);
		
		SemaphoreGatedWorkerStack stack = new SemaphoreGatedWorkerStack(mockSemaphore, config);
		// call under test
		stack.run();
		verify(mockRunner).run(any(ProgressCallback.class));
	}

	@Test
	public void testGateCanRunFalse() throws Exception{
		when(mockGate.canRun()).thenReturn(false);
		SemaphoreGatedWorkerStack stack = new SemaphoreGatedWorkerStack(mockSemaphore, config);
		// call under test
		stack.run();
		verify(mockRunner, never()).run(any(ProgressCallback.class));
	}
	
	@Test
	public void testRunNoSemaphoreLock() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		when(mockGate.canRun()).thenReturn(true);
		
		SemaphoreGatedWorkerStack stack = new SemaphoreGatedWorkerStack(mockSemaphore, config);
		// call under test
		stack.run();
		verify(mockRunner, never()).run(any(ProgressCallback.class));
	}
	
	@Test
	public void testNullGateHappy() throws Exception{
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(token));

		config.setGate(null);
		SemaphoreGatedWorkerStack stack = new SemaphoreGatedWorkerStack(mockSemaphore, config);
		// call under test
		stack.run();
		verify(mockRunner).run(any(ProgressCallback.class));
	}
	
	@Test
	public void testNullGateRunNoSemaphoreLock() throws Exception{
		config.setGate(null);
		when(mockSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		SemaphoreGatedWorkerStack stack = new SemaphoreGatedWorkerStack(mockSemaphore, config);
		// call under test
		stack.run();
		verify(mockRunner, never()).run(any(ProgressCallback.class));
	}
}
