package org.sagebionetworks.repo.web.filter.throttle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.throttle.UserRequestFrequencyThrottler.CLOUDWATCH_EVENT_NAME;
import static org.sagebionetworks.repo.web.filter.throttle.UserRequestFrequencyThrottler.MAX_REQUEST_FREQUENCY_LOCKS;
import static org.sagebionetworks.repo.web.filter.throttle.UserRequestFrequencyThrottler.REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC;

import javax.servlet.FilterChain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class UserRequestFrequencyThrottlerTest {

	UserRequestFrequencyThrottler throttler;

	@Mock
	private MemoryTimeBlockCountingSemaphore userFrequencyThrottleGate;

	@Mock
	private FilterChain filterChain;

	private static final String userId = "111";
	private static final String sessionId = "session-id";
	private static final String ipAddress = "123.123.123.123";

	private HttpRequestIdentifier requestIdentifier = new HttpRequestIdentifier(Long.valueOf(userId), sessionId, ipAddress, "/fakePath");
	private final String userMachineIdentifierString = requestIdentifier.getUserMachineIdentifierString();



	@Before
	public void setUp() throws Exception {
		throttler = new UserRequestFrequencyThrottler();

		ReflectionTestUtils.setField(throttler, "userThrottleMemoryTimeBlockSemaphore", userFrequencyThrottleGate);
		assertNotNull(userFrequencyThrottleGate);
	}

	@Test
	public void testNotThrottled() throws Exception {
		when(userFrequencyThrottleGate.attemptToAcquireLock(userMachineIdentifierString, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(true);

		RequestThrottlerCleanup cleanup = throttler.doThrottle(requestIdentifier);

		verify(userFrequencyThrottleGate).attemptToAcquireLock(userMachineIdentifierString, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
		verifyNoMoreInteractions(filterChain, userFrequencyThrottleGate);
		assertEquals(RequestThrottlerCleanupNoOpImpl.class, cleanup.getClass());

	}

	@Test
	public void testNoEmptyRequestFrequencySlots() throws Exception {
		when(userFrequencyThrottleGate.attemptToAcquireLock(userMachineIdentifierString, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(false);

		try {
			//method under test
			throttler.doThrottle(requestIdentifier);
			fail("Expected RequestThrottledException to be thrown");
		} catch (RequestThrottledException e){ //expected
			assertEquals(CLOUDWATCH_EVENT_NAME, e.getProfileData().getName());
		}

		verify(userFrequencyThrottleGate).attemptToAcquireLock(userMachineIdentifierString, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
	}
}
