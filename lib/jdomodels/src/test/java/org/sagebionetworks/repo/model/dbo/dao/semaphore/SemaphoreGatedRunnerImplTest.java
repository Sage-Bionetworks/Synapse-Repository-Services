package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.dao.semaphore.ProgressingRunner;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.TestClock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for SemaphoreGatedRunnerImpl
 *
 */
public class SemaphoreGatedRunnerImplTest {
	
	CountingSemaphore mockSemaphoreDao;
	TestClock testClock = new TestClock();
	String semaphoreKey;
	int maxNumberRunners;
	Runnable mockRunner;
	long timeoutMS;
	long timeoutSec;
	long currentTime;
	SemaphoreGatedRunnerImpl semaphoreGatedRunner;
	
	@Before
	public void before(){
		mockSemaphoreDao = Mockito.mock(CountingSemaphore.class);
		mockRunner = Mockito.mock(Runnable.class);
		semaphoreKey = "someKey";
		maxNumberRunners = 1;
		timeoutMS = 10*1000+1;
		timeoutSec = timeoutMS/1000l;
		semaphoreGatedRunner = new SemaphoreGatedRunnerImpl();
		semaphoreGatedRunner.setSemaphoreDao(mockSemaphoreDao);
		semaphoreGatedRunner.setSemaphoreKey(semaphoreKey);
		semaphoreGatedRunner.setMaxNumberRunners(maxNumberRunners);
		semaphoreGatedRunner.setTimeoutMS(timeoutMS);
		ReflectionTestUtils.setField(semaphoreGatedRunner, "clock", testClock);
	}

	@After
	public void after(){
		semaphoreGatedRunner.clearKeys();
	}
	
	@Test
	public void testLockNotAcquired(){
		String key = semaphoreKey+"-0";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey, timeoutSec, 1)).thenReturn(null);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(eq(key), anyString());
	}
	
	@Test
	public void testLockAcquired(){
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey, timeoutSec, 1)).thenReturn(token);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, times(1)).run();
		verify(mockSemaphoreDao, times(1)).releaseLock(semaphoreKey, token);
	}
	
	@Test
	public void testReleaseOnException(){
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey, timeoutSec, 1)).thenReturn(token);
		doThrow(new RuntimeException("something went horribly wrong!")).when(mockRunner).run();
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, times(1)).run();
		verify(mockSemaphoreDao, times(1)).releaseLock(semaphoreKey, token);
	}
	
	@Test
	public void testDoNotRunWhenMaxLessThanOne(){
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey, timeoutSec, 1)).thenReturn(token);
		semaphoreGatedRunner.setMaxNumberRunners(0);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(eq(semaphoreKey), anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testKeyTooLong(){
		char[] array = new char[31];
		Arrays.fill(array, 'a');
		String key = new String(array);
		semaphoreGatedRunner.setSemaphoreKey(key);
	}
	
	@Test
	public void testDuplicateKey(){
		SemaphoreGatedRunnerImpl gateOne = new SemaphoreGatedRunnerImpl();
		SemaphoreGatedRunnerImpl gateTwo = new SemaphoreGatedRunnerImpl();
		gateOne.setSemaphoreKey("duplicateKey");
		try{
			gateTwo.setSemaphoreKey("duplicateKey");
			fail("Should have failed with a duplicate key error");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("'duplicateKey' is already in use") > 0);
		}
	}
	
	@Test
	public void testMinTimeout(){
		try{
			semaphoreGatedRunner.setTimeoutMS(9*1000);
			fail("Should have failed since we set the timeout below the minimum");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("timeout is below the minimum timeout") > 0);
		}

	}

	@Test
	public void testProgressingRunnder(){
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey, timeoutSec, 1)).thenReturn(token);
		// run
		semaphoreGatedRunner.setRunner(new ProgressingRunner() {
			@Override
			public void run(ProgressCallback<Void> callback) throws Exception {
				// Not every call should result in a lock-refresh, only if enough time has expired
				testClock.warpForward(timeoutMS / 4);
				callback.progressMade(null);
				testClock.warpForward(timeoutMS / 4 - 1);
				callback.progressMade(null);

				// This should trigger a refresh
				testClock.warpForward(2);
				callback.progressMade(null);

				testClock.warpForward(timeoutMS / 2);
				callback.progressMade(null);
				// This should trigger a refresh
				testClock.warpForward(timeoutMS / 2);
				callback.progressMade(null);
			}
		});
		semaphoreGatedRunner.attemptToRun();
		verify(mockSemaphoreDao, times(1)).attemptToAcquireLock(semaphoreKey, timeoutSec, 1);
		verify(mockSemaphoreDao, times(1)).releaseLock(semaphoreKey, token);
		// The lock should only be refreshed 2 times even though callback.progressMade() was call more than that.
		verify(mockSemaphoreDao, times(2)).refreshLockTimeout(semaphoreKey, token, timeoutSec);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetBadRunnder(){
		semaphoreGatedRunner.setRunner(new Object());
	}
}
