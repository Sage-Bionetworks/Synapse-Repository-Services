package org.sagebionetworks.repo.model.semaphore;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class MempryTimeBlockCountingSemaphoreTest {
	
	MemoryTimeBlockCountingSemaphore memoryTimeBlockCountingSemaphore;
	
	private static String key = "some key";
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		memoryTimeBlockCountingSemaphore = Mockito.spy(new MemoryTimeBlockCountingSemaphoreImpl());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNullKey(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(null, 1, 1);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNegativeTimeoutSec(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, -1, 1);
		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNegativeMaxLock(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, -1);
	}

	@Test
	public void testAcquireLockNoExistentSemaphore() {
		boolean gotLock = memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1);
		assertTrue(gotLock);
	}
	
	@Test
	public void testAcquireLockExpiredSemaphore() throws InterruptedException {
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1));
		assertFalse(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1));
		Thread.sleep(2 * 1000);
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1));
	}
	
	@Test
	public void testAcquireLockOverCountLimit(){
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1));
		assertFalse(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 1));
	}
	
	@Test
	public void testAcquireLockUnderCountLimit(){
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 2));
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, 1, 2));

	}

}
