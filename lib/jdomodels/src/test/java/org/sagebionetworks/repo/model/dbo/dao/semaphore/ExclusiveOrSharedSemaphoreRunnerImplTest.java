package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreDao;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.springframework.test.util.ReflectionTestUtils;


public class ExclusiveOrSharedSemaphoreRunnerImplTest {

	
	ExclusiveOrSharedSemaphoreDao mockExclusiveOrSharedSemaphoreDao;
	ExclusiveOrSharedSemaphoreRunnerImpl runner;
	
	@Before
	public void before(){
		// Mock the dao
		mockExclusiveOrSharedSemaphoreDao = Mockito.mock(ExclusiveOrSharedSemaphoreDao.class);
		runner = new ExclusiveOrSharedSemaphoreRunnerImpl();
		ReflectionTestUtils.setField(runner, "exclusiveOrSharedSemaphoreDao", mockExclusiveOrSharedSemaphoreDao);
	}
	
	@Test
	public void testRunWithSharedLockHappy() throws Exception{
		String lockKey = "123";
		long timeout = 1000;
		String readToken = UUID.randomUUID().toString();
		when(mockExclusiveOrSharedSemaphoreDao.acquireSharedLock(lockKey, timeout)).thenReturn(readToken);
		
		String result = runner.tryRunWithSharedLock(lockKey, timeout, new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "okay";
			}
		});
		assertEquals("okay", result);
		// The lock must be acquired once and released once.
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).acquireSharedLock(lockKey, timeout);
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).releaseSharedLock(lockKey, readToken);
	}
	
	@Test
	public void testRunWithSharedLockReleaseOnException() throws Exception{
		String lockKey = "123";
		long timeout = 1000;
		String readToken = UUID.randomUUID().toString();
		when(mockExclusiveOrSharedSemaphoreDao.acquireSharedLock(lockKey, timeout)).thenReturn(readToken);
		
		try {
			runner.tryRunWithSharedLock(lockKey, timeout, new Callable<String>() {
				@Override
				public String call() throws Exception {
					throw new IllegalArgumentException("something went wrong");
				}
			});
			fail("An exception should have been thrown");
		} catch (IllegalArgumentException e) {
			// This is expected
		}
		// The lock must be acquired once and released once.
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).acquireSharedLock(lockKey, timeout);
		// The lock must be released even if there is an an exception.
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).releaseSharedLock(lockKey, readToken);
	}
	
	@Test
	public void testRunWithExclusiveLockHappy() throws Exception{
		String lockKey = "123";
		long timeout = 4000;
		String precursorToken = UUID.randomUUID().toString();
		String writeToken = UUID.randomUUID().toString();
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(lockKey)).thenReturn(precursorToken);
		// This is setup to not return the token the first two times to simulate a wait for a read lock.
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLock(lockKey, precursorToken, timeout)).thenReturn(null, null,writeToken);
		
		String result = runner.tryRunWithExclusiveLock(lockKey, timeout, new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "success";
			}
		});
		assertEquals("success", result);
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).acquireExclusiveLockPrecursor(lockKey);
		// There should be three attempts to get the write lock
		verify(mockExclusiveOrSharedSemaphoreDao, times(3)).acquireExclusiveLock(lockKey, precursorToken, timeout);
		// The lock should be released
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).releaseExclusiveLock(lockKey, writeToken);
	}
	
	@Test (expected=LockUnavilableException.class)
	public void testRunWithExclusiveLockTimeout() throws Exception{
		String lockKey = "123";
		long timeout = 1000;
		String precursorToken = UUID.randomUUID().toString();
		String writeToken = UUID.randomUUID().toString();
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(lockKey)).thenReturn(precursorToken);
		// Each time this is called we return null, to simulate waiting for a read-lock to be released.
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLock(lockKey, precursorToken, timeout)).thenReturn(null, null,null,null,null);
		// This should timeout because the timeout is set to 1000 and it will fail each time.  There is
		// a one second wait after each failure so a timeout should be triggered.  If the wait is changed
		// this test might need to be updated.
		String result = runner.tryRunWithExclusiveLock(lockKey, timeout, new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "success";
			}
		});
	}
	
	@Test
	public void testRunWithExclusiveLockException() throws Exception{
		String lockKey = "123";
		long timeout = 4000;
		String precursorToken = UUID.randomUUID().toString();
		String writeToken = UUID.randomUUID().toString();
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(lockKey)).thenReturn(precursorToken);
		// This is setup to not return the token the first two times to simulate a wait for a read lock.
		when(mockExclusiveOrSharedSemaphoreDao.acquireExclusiveLock(lockKey, precursorToken, timeout)).thenReturn(null, null,writeToken);
		
		try {
			runner.tryRunWithExclusiveLock(lockKey, timeout, new Callable<String>() {
				@Override
				public String call() throws Exception {
					throw new IllegalArgumentException("Something went wrong!");
				}
			});
			fail("An exception should have been thrown");
		} catch (IllegalArgumentException e) {
			// expected
		}

		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).acquireExclusiveLockPrecursor(lockKey);
		// There should be three attempts to get the write lock
		verify(mockExclusiveOrSharedSemaphoreDao, times(3)).acquireExclusiveLock(lockKey, precursorToken, timeout);
		// The lock should be released even though there was an exception.
		verify(mockExclusiveOrSharedSemaphoreDao, times(1)).releaseExclusiveLock(lockKey, writeToken);
	}
	
}
