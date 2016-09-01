package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.UserApiFrequencyThrottleFilter.CLOUDWATCH_EVENT_NAME;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.THROTTLED_HTTP_STATUS;

import javax.servlet.FilterChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils; 


@RunWith(MockitoJUnitRunner.class)
public class UserApiFrequencyThrottleFilterTest {
	
	private UserApiFrequencyThrottleFilter filter;
	
	@Mock
	ThrottleRulesCache throttleRulesCache;
	
	@Mock
	private FilterChain filterChain;
	
	@Mock
	private MemoryCountingSemaphore userThrottleGate;
	
	@Mock
	private MemoryTimeBlockCountingSemaphore userFrequencyThrottleGate;
	
	private static final String userId = "123";
	private static final String path = "/repo/v1/the/path/is/a/lie/12345/";
	private static final String normalizedPath = PathNormalizer.normalizeMethodSignature(path);
	private static final ThrottleLimit throttleLimit = new ThrottleLimit(123, 456);
	private static final String keyForSemaphore = userId + ":" + normalizedPath;
	
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	
	@Before
	public void setupFilter() throws Exception {
		filter = new UserApiFrequencyThrottleFilter();
		ReflectionTestUtils.setField(filter, "userApiThrottleMemoryTimeBlockSemaphore", userFrequencyThrottleGate);
		ReflectionTestUtils.setField(filter, "throttleRulesCache", throttleRulesCache);
		
		request = new MockHttpServletRequest();
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setRequestURI(path);
		
		response = new MockHttpServletResponse();
	}
	
	@Test
	public void testAnonymous() throws Exception {
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyZeroInteractions(userFrequencyThrottleGate);
		verifyNoMoreInteractions(filterChain);
	}
	
	@Test
	public void testMigrationAdmin() throws Exception{
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyZeroInteractions(userFrequencyThrottleGate);
		verifyNoMoreInteractions(filterChain);
	}
	
	@Test
	public void testNoThrottleForPath() throws Exception{
		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(null);
		
		filter.doFilter(request, response, filterChain);
		
		verify(throttleRulesCache).getThrottleLimit(normalizedPath);
		verify(filterChain).doFilter(request, response);
		verifyZeroInteractions(userFrequencyThrottleGate);
		verifyNoMoreInteractions(filterChain);
		
	}
	
	@Test
	public void testUserUnderThrottleLimit() throws Exception {
		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(throttleLimit);
		when(userFrequencyThrottleGate.attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod())).thenReturn(true);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userFrequencyThrottleGate).attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod());
		verifyNoMoreInteractions(filterChain, userFrequencyThrottleGate);
	}
	
	@Test
	public void testNoEmptyRequestFrequencySlots() throws Exception {
		Consumer consumer = mock(Consumer.class);
		ReflectionTestUtils.setField(filter, "consumer", consumer);
		
		when(throttleRulesCache.getThrottleLimit(normalizedPath)).thenReturn(throttleLimit);
		when(userFrequencyThrottleGate.attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod())).thenReturn(false);

		filter.doFilter(request, response, filterChain);
		assertEquals(THROTTLED_HTTP_STATUS, response.getStatus());
		
		ArgumentCaptor<ProfileData> profileDataArgument = ArgumentCaptor.forClass(ProfileData.class); 
		verify(consumer).addProfileData(profileDataArgument.capture());
		assertEquals(CLOUDWATCH_EVENT_NAME, profileDataArgument.getValue().getName());
		
		verify(userFrequencyThrottleGate).attemptToAcquireLock(keyForSemaphore, throttleLimit.getCallPeriodSec(), throttleLimit.getMaxCallsPerUserPerPeriod());
		verify(consumer).addProfileData(any(ProfileData.class));
		verifyNoMoreInteractions(filterChain, userFrequencyThrottleGate, consumer);
	}
}
