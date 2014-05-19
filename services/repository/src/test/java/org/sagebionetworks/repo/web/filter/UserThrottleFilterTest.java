package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreGatedRunner;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class UserThrottleFilterTest {

	private UserThrottleFilter filter;

	private SemaphoreGatedRunner userThrottleGate;

	@Before
	public void setupFilter() throws Exception {
		userThrottleGate = mock(SemaphoreGatedRunner.class);
		filter = new UserThrottleFilter();
		filter.setUserThrottleGate(userThrottleGate);
	}

	@Test
	public void testAnonymous() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyNoMoreInteractions(filterChain, userThrottleGate);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNotAnonymous() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "111");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		when(userThrottleGate.attemptToRunAllSlots(any(Callable.class))).thenAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				return ((Callable<Void>) invocation.getArguments()[0]).call();
			}
		});
		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(userThrottleGate).attemptToRunAllSlots(any(Callable.class));
		verifyNoMoreInteractions(filterChain, userThrottleGate);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = ServletException.class)
	public void testNoEmptySlots() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, "111");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		when(userThrottleGate.attemptToRunAllSlots(any(Callable.class))).thenThrow(new LockUnavilableException());
		try {
			filter.doFilter(request, response, filterChain);
		} finally {
			verify(userThrottleGate).attemptToRunAllSlots(any(Callable.class));
			verifyNoMoreInteractions(filterChain, userThrottleGate);
		}
	}
}
