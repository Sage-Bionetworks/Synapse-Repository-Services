package org.sagebionetworks.repo.throttle;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.common.util.Clock;

@RunWith(MockitoJUnitRunner.class)
public class CountingSemaphoreThrottleUnitTest {
	
	@Mock
	Clock mockClock;
	@Mock
	ProceedingJoinPoint mockPoint;
	@Mock
	Signature mockSignature;
	
	String result;
	
	@InjectMocks
	CountingSemaphoreThrottle throttle;
	
	@Before
	public void before() throws Throwable {
		when(mockClock.currentTimeMillis()).thenReturn(1L, 3L, 9L, 81L);
		result = "foo";
		when(mockPoint.getSignature()).thenReturn(mockSignature);
	}
	
	@Test
	public void testThrottle() throws Throwable {
		when(mockPoint.proceed()).thenReturn(result);
		// call under test
		Object back = throttle.profile(mockPoint);
		assertEquals(result, back);
		verify(mockClock).sleep((3-1));
		// one more time
		throttle.profile(mockPoint);
		verify(mockClock).sleep((81-9));
	}
	
	@Test
	public void testThrottleZeroElapse() throws Throwable {
		when(mockPoint.proceed()).thenReturn(result);
		// elapse should be zero
		when(mockClock.currentTimeMillis()).thenReturn(1L, 1L);
		// call under test
		Object back = throttle.profile(mockPoint);
		assertEquals(result, back);
		// with zero elapse so sleep.
		verify(mockClock, never()).sleep(any(Long.class));
	}
	
	@Test
	public void testThrottleNullResultNotAqcuire() throws Throwable {
		when(mockPoint.proceed()).thenReturn(null);
		when(mockSignature.getName()).thenReturn("refreshLockTimeout");
		// call under test
		Object back = throttle.profile(mockPoint);
		assertEquals(null, back);
		// Should sleep for only the elapse as not an acquire lock call.
		verify(mockClock).sleep((3-1));
	}
	
	@Test
	public void testThrottleFailedAcquireLock() throws Throwable {
		when(mockPoint.proceed()).thenReturn(null);
		when(mockSignature.getName()).thenReturn("attemptToAcquireLock");
		// call under test
		Object back = throttle.profile(mockPoint);
		assertEquals(null, back);
		// Should sleep for longer as this was a failure.
		verify(mockClock).sleep((3-1)*10);
	}

}
