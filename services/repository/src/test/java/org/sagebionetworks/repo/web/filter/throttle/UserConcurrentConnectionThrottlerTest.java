package org.sagebionetworks.repo.web.filter.throttle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.throttle.UserConcurrentConnectionThrottler.CLOUDWATCH_EVENT_NAME;
import static org.sagebionetworks.repo.web.filter.throttle.UserConcurrentConnectionThrottler.CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC;
import static org.sagebionetworks.repo.web.filter.throttle.UserConcurrentConnectionThrottler.MAX_CONCURRENT_LOCKS;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.test.util.ReflectionTestUtils;


@RunWith(MockitoJUnitRunner.class)
public class UserConcurrentConnectionThrottlerTest {

	private UserConcurrentConnectionThrottler throttler;

	@Mock
	private MemoryCountingSemaphore userThrottleGate;

	private static final String userId = "123";
	private static final String sessionId = "session-id";
	private static final String ipAddress = "123.123.123.123";
	private static final String concurrentSemaphoreToken = "concurrentToken";

	private HttpRequestIdentifier requestIdentifier = new HttpRequestIdentifier(Long.valueOf(userId), sessionId, ipAddress, "/fakePath");
	private final String userMachineIdentifierString = requestIdentifier.getUserMachineIdentifierString();

	@Before
	public void setup() throws Exception {
		throttler = new UserConcurrentConnectionThrottler();

		ReflectionTestUtils.setField(throttler, "userThrottleMemoryCountingSemaphore", userThrottleGate);
		assertNotNull(userThrottleGate);
	}

	@Test
	public void testNotThrottled() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(userMachineIdentifierString, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(concurrentSemaphoreToken);

		//method under test
		RequestThrottlerCleanup cleanup = throttler.doThrottle(requestIdentifier);

		verify(userThrottleGate).attemptToAcquireLock(userMachineIdentifierString, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);

		//check that returned clean object will call release lock
		cleanup.close();
		verify(userThrottleGate).releaseLock(userMachineIdentifierString,concurrentSemaphoreToken);
	}

	@Test
	public void testNoEmptyConcurrentConnectionSlots() throws Exception {

		when(userThrottleGate.attemptToAcquireLock(userMachineIdentifierString, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(null);

		try {
			//method under test
			throttler.doThrottle(requestIdentifier);
			fail("Expected RequestThrottledException to be thrown");
		} catch (RequestThrottledException e){ //expected
			assertEquals(CLOUDWATCH_EVENT_NAME, e.getProfileData().getName());
		}

		verify(userThrottleGate).attemptToAcquireLock(userMachineIdentifierString, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(userThrottleGate, never()).releaseLock(anyString(),anyString());
	}


}
