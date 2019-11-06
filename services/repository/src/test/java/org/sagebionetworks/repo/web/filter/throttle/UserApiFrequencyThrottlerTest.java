package org.sagebionetworks.repo.web.filter.throttle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.throttle.UserApiFrequencyThrottler.CLOUDWATCH_EVENT_NAME;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.test.util.ReflectionTestUtils;


@RunWith(MockitoJUnitRunner.class)
public class UserApiFrequencyThrottlerTest {

	private UserApiFrequencyThrottler throttler;

	@Mock
	ThrottleRulesCache throttleRulesCache;

	@Mock
	private MemoryCountingSemaphore userThrottleGate;

	@Mock
	private MemoryTimeBlockCountingSemaphore userFrequencyThrottleGate;

	private static final String userId = "123";
	private static final String sessionId = "session-id";
	private static final String ipAddress = "123.123.123.123";
	private static final String path = "/repo/v1/the/path/is/a/lie/12345/";
	private static final String normalizedPath = PathNormalizer.normalizeMethodSignature(path);
	private static final ThrottleLimit throttleLimit = new ThrottleLimit(123, 456);


	private HttpRequestIdentifier requestIdentifier = new HttpRequestIdentifier(Long.valueOf(userId), sessionId, ipAddress, path);
	private final String userMachineIdentifierString = requestIdentifier.getUserMachineIdentifierString();
	private final String keyForSemaphore = userMachineIdentifierString + ":" + normalizedPath;


	@Before
	public void setupFilter() throws Exception {
		throttler = new UserApiFrequencyThrottler();
		ReflectionTestUtils.setField(throttler, "userApiThrottleMemoryTimeBlockSemaphore", userFrequencyThrottleGate);
		ReflectionTestUtils.setField(throttler, "throttleRulesCache", throttleRulesCache);

	}

	@Test
	public void testNoThrottleForPath() throws Exception{
		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(null);

		//method under test
		RequestThrottlerCleanup cleanup = throttler.doThrottle(requestIdentifier);

		verify(throttleRulesCache).getThrottleLimit(normalizedPath);
		verifyZeroInteractions(userFrequencyThrottleGate);
		assertEquals(RequestThrottlerCleanupNoOpImpl.class, cleanup.getClass());
	}

	@Test
	public void testUserUnderThrottleLimit() throws Exception {
		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(throttleLimit);
		when(userFrequencyThrottleGate.attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod())).thenReturn(true);

		//method under test
		RequestThrottlerCleanup cleanup = throttler.doThrottle(requestIdentifier);

		verify(userFrequencyThrottleGate).attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod());
		assertEquals(RequestThrottlerCleanupNoOpImpl.class, cleanup.getClass());
	}

	@Test
	public void testNoEmptyRequestFrequencySlots() throws Exception {

		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(throttleLimit);
		when(userFrequencyThrottleGate.attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod())).thenReturn(false);

		try {
			//method under test
			throttler.doThrottle(requestIdentifier);
			fail("Expected RequestThrottledException to be thrown");
		}catch (RequestThrottledException e){ //expected
			assertEquals(CLOUDWATCH_EVENT_NAME, e.getProfileData().getName());
		}

		verify(userFrequencyThrottleGate).attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod());
	}
}
