package org.sagebionetworks.repo.model.semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;

public class MemoryCountingSemaphoreTest {
	
	@Mock
	Clock mockClock;
	
	MemoryCountingSemaphore memoryCountingSemaphore;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		memoryCountingSemaphore = new MemoryCountingSemaphore(mockClock);
		// setup some default clock behavior
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 2000L,3000L,4000L,5000L,6000L);
	}
	
	@Test
	public void testAttemptToAcquireLock(){
		String key = "someKey";
		int maxLockCount = 2;
		long timeoutSec = 10;
		String token = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		String token2 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token2);
		assertFalse(token.equals(token2));
		String token3 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertEquals("should not be able to get a third token",null, token3);
	}
	
	@Test
	public void testAttemptToAcquireLockRemoveExpired(){
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 3000L,5000L,7000L);
		String key = "someKey";
		int maxLockCount = 1;
		long timeoutSec = 1;
		String token = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		String token2 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull("The first lock should be expired so we should be able to get another.", token2);
		// validate the first lock is expired
		try{
			memoryCountingSemaphore.refreshLockTimeout(key, token, timeoutSec);
			fail("LockReleaseFailedException expected");
		}catch(LockReleaseFailedException e){
			// expected
		}
	}
	
	@Test
	public void testRefreshLockTimeout(){
		when(mockClock.currentTimeMillis()).thenReturn(1000L, 3000L,5000L,7000L);
		String key = "someKey";
		int maxLockCount = 1;
		long timeoutSec = 1;
		String token = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		// refresh the lock.
		memoryCountingSemaphore.refreshLockTimeout(key, token, timeoutSec*10);
		// Should fail to get the lock now.
		String token2 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertEquals("The first lock should not be expired since it was refreshed so should not get another.", null, token2);
	}
	
	@Test
	public void testReleaseLock(){
		String key = "someKey";
		int maxLockCount = 1;
		long timeoutSec = 1000;
		String token = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		String token2 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertEquals("should not be able to get the lock as it is not expired.", null, token2);
		// release the lock
		memoryCountingSemaphore.releaseLock(key, token);
		token2 = memoryCountingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull("Should be able to get the lock after released.", token2);
		// release the lock again should fail
		try{
			memoryCountingSemaphore.releaseLock(key, token);
			fail("LockReleaseFailedException expected");
		}catch(LockReleaseFailedException e){
			// expected
		}
	}

}
