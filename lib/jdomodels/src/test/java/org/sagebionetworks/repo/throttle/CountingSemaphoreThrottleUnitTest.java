package org.sagebionetworks.repo.throttle;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.Clock;

@ExtendWith(MockitoExtension.class)
public class CountingSemaphoreThrottleUnitTest {
	
	@Mock
	private Clock mockClock;
	@Mock
	private ProceedingJoinPoint mockPoint;
	@Mock
	private Signature mockSignature;
	
	private Optional<String> result;
	
	@InjectMocks
	CountingSemaphoreThrottle throttle;
	
	@BeforeEach
	public void before() throws Throwable {
		when(mockClock.currentTimeMillis()).thenReturn(1L, 3L, 9L, 81L);
		result = Optional.of("foo");
	}
	
	@Test
	public void testThrottle() throws Throwable {
		when(mockPoint.getSignature()).thenReturn(mockSignature);
		when(mockSignature.getName()).thenReturn("releaseLock");
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
		when(mockPoint.getSignature()).thenReturn(mockSignature);
		when(mockSignature.getName()).thenReturn("releaseLock");
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
		when(mockPoint.proceed()).thenReturn(Optional.empty());
		when(mockPoint.getSignature()).thenReturn(mockSignature);
		when(mockSignature.getName()).thenReturn("refreshLockTimeout");
		// call under test
		Optional<String> back = (Optional<String>) throttle.profile(mockPoint);
		assertEquals(Optional.empty(), back);
		// Should sleep for only the elapse as not an acquire lock call.
		verify(mockClock).sleep((3-1));
	}
	
	@Test
	public void testThrottleFailedAcquireLock() throws Throwable {
		when(mockPoint.proceed()).thenReturn(Optional.empty());
		when(mockPoint.getSignature()).thenReturn(mockSignature);
		when(mockSignature.getName()).thenReturn("attemptToAcquireLock");
		// call under test
		Optional<String> back = (Optional<String>) throttle.profile(mockPoint);
		assertEquals(Optional.empty(), back);
		// Should sleep for longer as this was a failure.
		verify(mockClock).sleep((3-1)*10);
	}
	
	@Test
	public void testThrottleAcquireLock() throws Throwable {
		when(mockPoint.proceed()).thenReturn(Optional.of("token"));
		when(mockPoint.getSignature()).thenReturn(mockSignature);
		when(mockSignature.getName()).thenReturn("attemptToAcquireLock");
		// call under test
		Optional<String> back = (Optional<String>) throttle.profile(mockPoint);
		assertEquals(Optional.of("token"), back);
		verify(mockClock).sleep((3-1));
	}
	
	@Test
	public void testThrottleAcquireLockWithException() throws Throwable {
		Exception exception = new IllegalArgumentException("wrong stuff");
		when(mockPoint.proceed()).thenThrow(exception);
		Exception result = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			 throttle.profile(mockPoint);
		});
		assertEquals(exception, result);
		verify(mockClock).sleep((3-1));
	}


}
