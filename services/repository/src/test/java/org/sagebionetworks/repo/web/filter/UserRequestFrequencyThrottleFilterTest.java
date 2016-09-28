package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.UserRequestFrequencyThrottleFilter.REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC;
import static org.sagebionetworks.repo.web.filter.UserRequestFrequencyThrottleFilter.MAX_REQUEST_FREQUENCY_LOCKS;
import static org.sagebionetworks.repo.web.filter.UserRequestFrequencyThrottleFilter.CLOUDWATCH_EVENT_NAME;
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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class UserRequestFrequencyThrottleFilterTest {
	
	UserRequestFrequencyThrottleFilter filter;
	
	@Mock
	private MemoryTimeBlockCountingSemaphore userFrequencyThrottleGate;
	
	@Mock
	private FilterChain filterChain;
	
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	
	private static final String userId = "111";
	
	@Before
	public void setUp() throws Exception {
		filter = new UserRequestFrequencyThrottleFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		ReflectionTestUtils.setField(filter, "userThrottleMemoryTimeBlockSemaphore", userFrequencyThrottleGate);
		assertNotNull(userFrequencyThrottleGate);
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
	public void testRegularUser() throws Exception {
		when(userFrequencyThrottleGate.attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(true);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userFrequencyThrottleGate).attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
		verifyNoMoreInteractions(filterChain, userFrequencyThrottleGate);
	}
	
	@Test
	public void testNoEmptyRequestFrequencySlots() throws Exception {
		Consumer consumer = mock(Consumer.class);
		ReflectionTestUtils.setField(filter, "consumer", consumer);
		
		when(userFrequencyThrottleGate.attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(false);

		filter.doFilter(request, response, filterChain);
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		assertEquals(THROTTLED_HTTP_STATUS, response.getStatus());
		
		ArgumentCaptor<ProfileData> profileDataArgument = ArgumentCaptor.forClass(ProfileData.class); 
		verify(consumer).addProfileData(profileDataArgument.capture());
		assertEquals(CLOUDWATCH_EVENT_NAME, profileDataArgument.getValue().getName());
		
		verify(userFrequencyThrottleGate).attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
		verify(consumer).addProfileData(any(ProfileData.class));
		verifyNoMoreInteractions(filterChain, userFrequencyThrottleGate, consumer);
	}
}
