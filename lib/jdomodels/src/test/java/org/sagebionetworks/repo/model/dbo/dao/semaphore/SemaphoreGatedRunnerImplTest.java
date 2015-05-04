package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.semaphore.ProgressingRunner;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.TestClock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for SemaphoreGatedRunnerImpl
 *
 */
public class SemaphoreGatedRunnerImplTest {
	
	SemaphoreDao mockSemaphoreDao;
	TestClock testClock = new TestClock();
	String semaphoreKey;
	int maxNumberRunners;
	Runnable mockRunner;
	long timeoutMS;
	long currentTime;
	SemaphoreGatedRunnerImpl semaphoreGatedRunner;
	
	@Before
	public void before(){
		mockSemaphoreDao = Mockito.mock(SemaphoreDao.class);
		mockRunner = Mockito.mock(Runnable.class);
		semaphoreKey = "someKey";
		maxNumberRunners = 1;
		timeoutMS = 10*1000+1;
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
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(null);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(eq(key), anyString());
	}
	
	@Test
	public void testLockAcquired(){
		String key = semaphoreKey+"-0";
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(token);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, times(1)).run();
		verify(mockSemaphoreDao, times(1)).releaseLock(key, token);
	}
	
	@Test
	public void testReleaseOnException(){
		String key = semaphoreKey+"-0";
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(token);
		doThrow(new RuntimeException("something went horribly wrong!")).when(mockRunner).run();
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, times(1)).run();
		verify(mockSemaphoreDao, times(1)).releaseLock(key, token);
	}
	
	@Test
	public void testDoNotRunWhenMaxLessThanOne(){
		String key = semaphoreKey+"-0";
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(token);
		semaphoreGatedRunner.setMaxNumberRunners(0);
		// run
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(eq(key), anyString());
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
	public void testRunAllSucceedFirst() throws Exception {
		// this sequence will basically invert the key set, so 1, then 0
		Random randomGen = Mockito.mock(Random.class);
		when(randomGen.nextInt(2)).thenReturn(0);
		ReflectionTestUtils.setField(semaphoreGatedRunner, "randomGen", randomGen);
		semaphoreGatedRunner.setMaxNumberRunners(2);

		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-1", timeoutMS)).thenReturn(token);
		// run
		Callable<Void> call = Mockito.mock(Callable.class);
		semaphoreGatedRunner.attemptToRunAllSlots(call, null);
		verify(call).call();
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-1", timeoutMS);
		verify(mockSemaphoreDao).releaseLock(semaphoreKey + "-1", token);
		verifyNoMoreInteractions(mockSemaphoreDao);
	}

	@Test
	public void testRunAllSucceedSecond() throws Exception {
		// this sequence will basically invert the key set, so 1, then 0
		Random randomGen = Mockito.mock(Random.class);
		when(randomGen.nextInt(2)).thenReturn(0);
		ReflectionTestUtils.setField(semaphoreGatedRunner, "randomGen", randomGen);
		semaphoreGatedRunner.setMaxNumberRunners(2);

		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-1", timeoutMS)).thenReturn(null);
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-0", timeoutMS)).thenReturn(token);
		// run
		Callable<Void> call = Mockito.mock(Callable.class);
		semaphoreGatedRunner.attemptToRunAllSlots(call, null);
		verify(call).call();
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-1", timeoutMS);
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-0", timeoutMS);
		verify(mockSemaphoreDao).releaseLock(semaphoreKey + "-0", token);
		verify(mockSemaphoreDao, never()).releaseLock(eq(semaphoreKey + "-1"), anyString());
		verifyNoMoreInteractions(mockSemaphoreDao);
	}

	@Test(expected = LockUnavilableException.class)
	public void testRunAllFail() throws Exception {
		// this sequence will basically invert the key set, so 1, then 0
		Random randomGen = Mockito.mock(Random.class);
		when(randomGen.nextInt(2)).thenReturn(0);
		ReflectionTestUtils.setField(semaphoreGatedRunner, "randomGen", randomGen);
		semaphoreGatedRunner.setMaxNumberRunners(2);

		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-1", timeoutMS)).thenReturn(null);
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-0", timeoutMS)).thenReturn(null);
		// run
		Callable<Void> call = Mockito.mock(Callable.class);
		try {
			semaphoreGatedRunner.attemptToRunAllSlots(call, null);
		} finally {
			verify(call, never()).call();
			verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-1", timeoutMS);
			verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-0", timeoutMS);
			verify(mockSemaphoreDao, never()).releaseLock(anyString(), anyString());
			verifyNoMoreInteractions(mockSemaphoreDao);
		}
	}

	@Test
	public void testRunIndependent() throws Exception {
		// this sequence will basically invert the key set, so 1, then 0
		Random randomGen = Mockito.mock(Random.class);
		when(randomGen.nextInt(2)).thenReturn(0);
		ReflectionTestUtils.setField(semaphoreGatedRunner, "randomGen", randomGen);
		semaphoreGatedRunner.setMaxNumberRunners(2);

		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-yy" + "-0", timeoutMS)).thenReturn(null);
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-yy" + "-1", timeoutMS)).thenReturn(null);
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-xx" + "-1", timeoutMS)).thenReturn(null);
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(semaphoreKey + "-xx" + "-0", timeoutMS)).thenReturn(token);
		// run
		Callable<Void> call1 = Mockito.mock(Callable.class);
		Callable<Void> call2 = Mockito.mock(Callable.class);
		semaphoreGatedRunner.attemptToRunAllSlots(call1, "xx");
		try {
			semaphoreGatedRunner.attemptToRunAllSlots(call2, "yy");
			fail("Should have failed");
		} catch (LockUnavilableException e) {
		}
		verify(call1).call();
		verify(call2, never()).call();
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-yy" + "-1", timeoutMS);
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-yy" + "-0", timeoutMS);
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-xx" + "-1", timeoutMS);
		verify(mockSemaphoreDao).attemptToAcquireLock(semaphoreKey + "-xx" + "-0", timeoutMS);
		verify(mockSemaphoreDao).releaseLock(semaphoreKey + "-xx" + "-0", token);
		verify(mockSemaphoreDao, never()).releaseLock(eq(semaphoreKey + "-xx" + "-1"), anyString());
		verify(mockSemaphoreDao, never()).releaseLock(eq(semaphoreKey + "-yy" + "-1"), anyString());
		verifyNoMoreInteractions(mockSemaphoreDao);
	}
	
	@Test
	public void testProgressingRunnder(){
		String key = semaphoreKey+"-0";
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(token);
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
		verify(mockSemaphoreDao, times(1)).attemptToAcquireLock(key, timeoutMS);
		verify(mockSemaphoreDao, times(1)).releaseLock(key, token);
		// The lock should only be refreshed 2 times even though callback.progressMade() was call more than that.
		verify(mockSemaphoreDao, times(2)).refreshLockTimeout(key, token, timeoutMS);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testSetBadRunnder(){
		semaphoreGatedRunner.setRunner(new Object());
	}
}
