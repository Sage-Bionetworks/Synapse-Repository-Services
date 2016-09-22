package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.sagebionetworks.repo.web.filter.UserConcurrentConnectionThrottleFilter.CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC;
import static org.sagebionetworks.repo.web.filter.UserConcurrentConnectionThrottleFilter.MAX_CONCURRENT_LOCKS;
import static org.sagebionetworks.repo.web.filter.UserConcurrentConnectionThrottleFilter.CLOUDWATCH_EVENT_NAME;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.THROTTLED_HTTP_STATUS;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

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
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;


@RunWith(MockitoJUnitRunner.class)
public class UserConcurrentConnectionThrottleFilterTest {

	private UserConcurrentConnectionThrottleFilter filter;
	
	@Mock
	private MemoryCountingSemaphore userThrottleGate;
	
	@Mock
	private FilterChain filterChain;
	
	private static final String userId = "123";
	private static final String concurrentSemaphoreToken = "concurrentToken";
	
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	@Before
	public void setupFilter() throws Exception {
		filter = new UserConcurrentConnectionThrottleFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		
		ReflectionTestUtils.setField(filter, "userThrottleMemoryCountingSemaphore", userThrottleGate);
		assertNotNull(userThrottleGate);
	}

	@Test
	public void testAnonymous() throws Exception {
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyZeroInteractions(userThrottleGate);
		verifyNoMoreInteractions(filterChain);
	}
	
	@Test
	public void testMigrationAdmin() throws Exception{
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());
		
		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyZeroInteractions(userThrottleGate);
		verifyNoMoreInteractions(filterChain);
	}

	@Test
	public void testRegularUser() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(concurrentSemaphoreToken);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userThrottleGate).attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(userThrottleGate).releaseLock(userId, concurrentSemaphoreToken);
		verifyNoMoreInteractions(filterChain, userThrottleGate);
	}

	@Test(expected = ServletException.class)
	public void testConcurrentConnectionsAcquireLockException() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenThrow(new RuntimeException());
		try {
			filter.doFilter(request, response, filterChain);
		} finally {
			verify(userThrottleGate).attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
			verifyNoMoreInteractions(filterChain, userThrottleGate);
		}
	}

	@Test
	public void testNoEmptyConcurrentConnectionSlots() throws Exception {
		Consumer consumer = mock(Consumer.class);
		ReflectionTestUtils.setField(filter, "consumer", consumer);

		when(userThrottleGate.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(null);
		
		filter.doFilter(request, response, filterChain);
		assertEquals(THROTTLED_HTTP_STATUS, response.getStatus());
		
		ArgumentCaptor<ProfileData> profileDataArgument = ArgumentCaptor.forClass(ProfileData.class); 
		verify(consumer).addProfileData(profileDataArgument.capture());
		assertEquals(CLOUDWATCH_EVENT_NAME, profileDataArgument.getValue().getName());
		
		verify(userThrottleGate).attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(consumer).addProfileData(any(ProfileData.class));
		verifyNoMoreInteractions(filterChain, userThrottleGate, consumer);
	}
	

}
