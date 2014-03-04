package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.BatchUpdateException;

import org.apache.commons.logging.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.dao.DeadlockLoserDataAccessException;

public class DeadlockWatcherTest {
	
	@Test
	public void testNoDeadLock() throws Throwable{
		DeadlockWatcher watcher = new DeadlockWatcher();
		ProceedingJoinPoint mockPoint = Mockito.mock(ProceedingJoinPoint.class);
		String expected = "Okay";
		// no deadlock should be a pass through.
		when(mockPoint.proceed()).thenReturn(expected);
		// Make the call
		Object result = watcher.detectDeadlock(mockPoint);
		assertTrue(result instanceof String);
		String stringResults = (String) result;
		assertEquals(expected, stringResults );
	}
	
	@Test
	public void testDeadlock() throws Throwable{
		DeadlockWatcher watcher = new DeadlockWatcher();
		Log mockLog = Mockito.mock(Log.class);
		watcher.setLog(mockLog);
		ProceedingJoinPoint mockPoint = Mockito.mock(ProceedingJoinPoint.class);
		String expected = "Okay";
		// This time simulate deadlock
		DeadlockLoserDataAccessException exception =new DeadlockLoserDataAccessException("Some kind of deadlock", new BatchUpdateException());
		when(mockPoint.proceed()).thenThrow(exception);
		// Make the call
		try{
			Object result = watcher.detectDeadlock(mockPoint);
			fail("This should have thrown an exception");
		}catch(DeadlockLoserDataAccessException e){
			// this is expected
		}
		// The following should get logged
		String[] expectedLogs = new String[]{
			String.format(DeadlockWatcher.START_MESSAGE, exception.getMessage()),
			String.format(DeadlockWatcher.FAILED_ATTEMPT, 2, 10),
			String.format(DeadlockWatcher.FAILED_ATTEMPT, 3, 10*10),
			String.format(DeadlockWatcher.FAILED_ATTEMPT, 4, 100*100),
			String.format(DeadlockWatcher.EXASTED_ALL_ATTEMPTS, 4),
		};
		// Verify we get each entry in the log

		for(String message: expectedLogs){
			System.out.println(message);
			verify(mockLog, times(1)).debug(message);
		}
	}
	
	

}
