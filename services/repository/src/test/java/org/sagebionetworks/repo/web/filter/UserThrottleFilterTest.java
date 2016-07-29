package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.MAX_CONCURRENT_LOCKS;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.MAX_REQUEST_FREQUENCY_LOCKS;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.CONCURRENT_CONNECTION_KEY_PREFIX;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.REQUEST_FREQUENCY_KEY_PREFIX;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.apache.http.HttpStatus;
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
public class UserThrottleFilterTest {

	private UserThrottleFilter filter;
	
	@Mock
	private MemoryCountingSemaphore userThrottleGate;
	
	@Mock
	private FilterChain filterChain;
	
	private static final String userId = "111";
	private static final String concurrentSemaphoreToken = "concurrentToken";
	private static final String frequencySemaphoreToken = "frequencyToken";
	
	private static final String concurrentKeyUserId = CONCURRENT_CONNECTION_KEY_PREFIX + userId;
	private static final String frequencyKeyUserId = REQUEST_FREQUENCY_KEY_PREFIX + userId;
	
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	@Before
	public void setupFilter() throws Exception {
		filter = new UserThrottleFilter();
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
		verifyNoMoreInteractions(filterChain, userThrottleGate);
	}

	@Test
	public void testNotAnonymous() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(concurrentSemaphoreToken);
		when(userThrottleGate.attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(frequencySemaphoreToken);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userThrottleGate).attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(userThrottleGate).attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);

		verify(userThrottleGate).releaseLock(concurrentKeyUserId, concurrentSemaphoreToken);
		
		verifyNoMoreInteractions(filterChain, userThrottleGate);
	}

	@Test(expected = ServletException.class)
	public void testConcurrentConnectionsAcquireLockException() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenThrow(new RuntimeException());
		try {
			filter.doFilter(request, response, filterChain);
		} finally {
			verify(userThrottleGate).attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
			verifyNoMoreInteractions(filterChain, userThrottleGate);
		}
	}
	
	@Test(expected = ServletException.class)
	public void testRequestFrequencyAcquireLockException() throws Exception {
		when(userThrottleGate.attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenThrow(new RuntimeException());
		try {
			filter.doFilter(request, response, filterChain);
		} finally {
			verify(userThrottleGate).attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
			verifyNoMoreInteractions(filterChain, userThrottleGate);
		}
	}

	@Test
	public void testNoEmptyConcurrentConnectionSlots() throws Exception {
		Consumer consumer = mock(Consumer.class);
		ReflectionTestUtils.setField(filter, "consumer", consumer);

		when(userThrottleGate.attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(null);
		when(userThrottleGate.attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(frequencySemaphoreToken);

		
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatus());
		
		ArgumentCaptor<ProfileData> profileDataArgument = ArgumentCaptor.forClass(ProfileData.class); 
		verify(consumer).addProfileData(profileDataArgument.capture());
		assertEquals("ConcurrentConnectionsLockUnavailable", profileDataArgument.getValue().getName());
		

		verify(userThrottleGate).attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(consumer).addProfileData(any(ProfileData.class));
		verifyNoMoreInteractions(filterChain, userThrottleGate, userThrottleGate, consumer);
	}
	
	@Test
	public void testNoEmptyRequestFrequencySlots() throws Exception {
		Consumer consumer = mock(Consumer.class);
		ReflectionTestUtils.setField(filter, "consumer", consumer);
		
		when(userThrottleGate.attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS)).thenReturn(concurrentSemaphoreToken);
		when(userThrottleGate.attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS)).thenReturn(null);

		filter.doFilter(request, response, filterChain);
		assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatus());
		
		ArgumentCaptor<ProfileData> profileDataArgument = ArgumentCaptor.forClass(ProfileData.class); 
		verify(consumer).addProfileData(profileDataArgument.capture());
		assertEquals("RequestFrequencyLockUnavailable", profileDataArgument.getValue().getName());
		
		
		verify(userThrottleGate).attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
		verify(userThrottleGate).attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
		verify(userThrottleGate).releaseLock(concurrentKeyUserId, concurrentSemaphoreToken);
		verify(consumer).addProfileData(any(ProfileData.class));
		verifyNoMoreInteractions(filterChain, userThrottleGate, userThrottleGate, consumer);
	}
}
