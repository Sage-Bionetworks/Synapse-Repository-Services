package org.sagebionetworks.repo.web.filter.throttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.web.HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class RequestThrottleFilterTest {
	@Mock
	private RequestThrottler mockRequestThrottler;
	@Mock
	private FilterChain mockFilterChain;
	@Mock
	private RequestThrottlerCleanup mockRequestThrottlerCleanup;
	@Mock
	private Consumer mockConsumer;
	@Mock
	private OpenIDConnectManager mockOidcManager;

	private MockHttpServletRequest mockRequest;
	private MockHttpServletResponse mockResponse;

	private static final String userId = "42";
	private static final String ipAddress = "192.168.1.1";
	private static final String sessionId = "69203fe7-a9ea-434b-a420-61294402072b";
	private static final String path ="/some/Path";
	
	private static final String ACCESS_TOKEN = "access-token";

	//class being tested
	@InjectMocks
	private RequestThrottleFilter filter;

	@BeforeEach
	public void setUp() throws Exception {
	    MockitoAnnotations.initMocks(this);
		
		mockRequest = new MockHttpServletRequest();
		mockResponse = new MockHttpServletResponse();

		//set up request identifiers in the mock request
		mockRequest.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		mockRequest.setRemoteAddr(ipAddress);
		mockRequest.setRequestURI(path);
		mockRequest.setCookies(new Cookie(SESSION_ID_COOKIE_NAME, sessionId));
		mockRequest.addHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME, "Bearer "+ACCESS_TOKEN);
		
		when(mockOidcManager.getUserId(ACCESS_TOKEN)).thenReturn(userId);
	}

	@Test
	public void testMigrationAdmin() throws Exception{
		when(mockOidcManager.getUserId(ACCESS_TOKEN)).thenReturn(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString());

		//method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verifyZeroInteractions(mockRequestThrottler);
		verifyZeroInteractions(mockRequestThrottlerCleanup);
		verifyNoMoreInteractions(mockFilterChain);
	}

	@Test
	public void testAnonymousUser() throws Exception{ //TODO: remove once java client has a way to get session id from cookies
		when(mockOidcManager.getUserId(ACCESS_TOKEN)).thenReturn(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());

		//method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verifyZeroInteractions(mockRequestThrottler);
		verifyZeroInteractions(mockRequestThrottlerCleanup);
		verifyNoMoreInteractions(mockFilterChain);
	}


	@Test
	public void testThrottlerThrottled() throws Exception {
		String throttleMessage = "You got throttled";
		ProfileData profileData = new ProfileData();
		when(mockRequestThrottler.doThrottle(any(HttpRequestIdentifier.class)))
				.thenThrow(new RequestThrottledException(throttleMessage, profileData));

		//method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockRequestThrottler).doThrottle(any(HttpRequestIdentifier.class));
		verify(mockConsumer).addProfileData(profileData);
		assertEquals(THROTTLED_HTTP_STATUS, mockResponse.getStatus());
		assertEquals(throttleMessage, mockResponse.getContentAsString().trim());

		verifyZeroInteractions(mockRequestThrottlerCleanup);
		verifyZeroInteractions(mockFilterChain);
	}

	@Test
	public void testThrottlerPassed() throws Exception {
		when(mockRequestThrottler.doThrottle(any(HttpRequestIdentifier.class))).thenReturn(mockRequestThrottlerCleanup);

		//method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verify(mockRequestThrottler).doThrottle(any(HttpRequestIdentifier.class));
		verify(mockRequestThrottlerCleanup).close();
		verifyNoMoreInteractions(mockFilterChain);
		verifyZeroInteractions(mockConsumer);
	}
}
