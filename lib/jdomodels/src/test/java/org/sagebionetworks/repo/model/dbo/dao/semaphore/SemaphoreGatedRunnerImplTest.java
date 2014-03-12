package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;

/**
 * Test for SemaphoreGatedRunnerImpl
 *
 */
public class SemaphoreGatedRunnerImplTest {
	
	SemaphoreDao mockSemaphoreDao;
	String semaphoreKey;
	int maxNumberRunners;
	Runnable mockRunner;
	long timeoutMS;
	
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
		semaphoreGatedRunner.setRunner(mockRunner);
		semaphoreGatedRunner.setSemaphoreKey(semaphoreKey);
		semaphoreGatedRunner.setMaxNumberRunners(maxNumberRunners);
		semaphoreGatedRunner.setTimeoutMS(timeoutMS);
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
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(key, null);
	}
	
	@Test
	public void testLockAcquired(){
		String key = semaphoreKey+"-0";
		String token = "someToken";
		when(mockSemaphoreDao.attemptToAcquireLock(key, timeoutMS)).thenReturn(token);
		// run
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
		semaphoreGatedRunner.attemptToRun();
		verify(mockRunner, never()).run();
		verify(mockSemaphoreDao, never()).releaseLock(key, null);
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
}
