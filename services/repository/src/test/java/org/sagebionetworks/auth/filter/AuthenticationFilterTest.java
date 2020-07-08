package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.repo.web.OAuthUnauthenticatedException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith({MockitoExtension.class})
public class AuthenticationFilterTest {
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private PrintWriter mockPrintWriter;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private OIDCTokenHelper oidcTokenHelper;
	
	@Mock
	private OpenIDConnectManager mockOidcManager;
	
	@Captor
	private ArgumentCaptor<HttpServletRequest> requestCaptor;

	@Mock
	private AuthenticationService mockAuthService;
	
	@Mock
	private UserManager mockUserManager;
	
	@InjectMocks
	private AuthenticationFilter filter;

	private static final String sessionToken = "someSortaSessionToken";
	private static final String secretKey = "Totally a plain text key :D";
	private static final String username = "AuthFilter@test.sagebase.org";
	private static final Long userId = 123456789L;
	private static final String BEARER_TOKEN = "bearer token";
	private static final String BEARER_TOKEN_HEADER = "Bearer "+BEARER_TOKEN;
	private static final List<String> HEADER_NAMES = Collections.singletonList("Authorization");
	private PrincipalAlias pa;
	
	@BeforeEach
	public void setupFilter() throws Exception {
		pa = new PrincipalAlias();
		pa.setPrincipalId(userId);

		filter.init(new FilterConfig() {
			public String getFilterName() { 
				return ""; 
			}
			
			public String getInitParameter(String name) {
				return null;
			}
			
			public Enumeration<String> getInitParameterNames() {
				return Collections.emptyEnumeration();
			}
			
			public ServletContext getServletContext() {
				return null;
			}
		});
	}
	
	@Test
	public void testAnonymous() throws Exception {
		// A request with no information provided
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);
		
		// Should default to anonymous
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String anonymous = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString(), anonymous);
	}
	
	/**
	 * Test that we treat an empty session token the same an a null session token.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_2422() throws Exception {
		// A request with no information provided
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, "");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();

		filter.doFilter(request, response, filterChain);
		
		// Should default to anonymous
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String anonymous = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertTrue(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString().equals(anonymous));
	}
	
	@Test
	public void testSessionToken_asHeader() throws Exception {
		when(mockAuthService.revalidate(eq(sessionToken), eq(false))).thenReturn(userId);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		when(oidcTokenHelper.createTotalAccessToken(userId)).thenReturn(BEARER_TOKEN);

		// method under test
		filter.doFilter(request, response, filterChain);
		
		// Session token should be recognized
		verify(mockAuthService, times(1)).revalidate(eq(sessionToken), eq(false));
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String sessionUserId = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(userId.toString(), sessionUserId);
	}
	
	@Test
	public void testSessionToken_asParameter() throws Exception {
		when(mockAuthService.revalidate(eq(sessionToken), eq(false))).thenReturn(userId);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		when(oidcTokenHelper.createTotalAccessToken(userId)).thenReturn(BEARER_TOKEN);

		// method under test
		filter.doFilter(request, response, filterChain);

		// Session token should be recognized
		verify(mockAuthService, times(1)).revalidate(eq(sessionToken), eq(false));
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String sessionUserId = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(userId.toString(), sessionUserId);
	}
	
	@Test
	public void testSessionToken_isNull() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(AuthorizationConstants.SESSION_TOKEN_PARAM, (String) null);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);

		// Session token should not be recognized
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String sessionUsername = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString(), sessionUsername);
	}
	
	@Test
	public void testHmac() throws Exception {
		when(mockAuthService.getSecretKey(eq(userId))).thenReturn(secretKey);
		when(mockUserManager.lookupUserByUsernameOrEmail(eq(username))).thenReturn(pa);

		MockHttpServletRequest request = new MockHttpServletRequest();
		String timestamp = new DateTime().toString();
		signRequest(request, "/foo/bar/baz", username, secretKey, timestamp);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		when(oidcTokenHelper.createTotalAccessToken(userId)).thenReturn(BEARER_TOKEN);

		// method under test
		filter.doFilter(request, response, filterChain);

		// Signature should match
		verify(mockAuthService, times(1)).getSecretKey(eq(userId));
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String passedAlongUsername = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(userId.toString(), passedAlongUsername);
	}
	
	@Test
	public void testMatchHMACSHA1SignatureOutOfDate() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		DateTime dt = new DateTime();
		dt = dt.minusMinutes(35);
		String timestamp = dt.toString();
		signRequest(request, "/foo/bar/baz", username, secretKey, timestamp);
		Assertions.assertThrows(UnauthenticatedException.class, ()->{
			AuthenticationFilter.matchHMACSHA1Signature(request, secretKey);
		});
	}

	@Test
	public void testMatchHMACSHA1Signature() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		DateTime dt = new DateTime();
		dt = dt.minusMinutes(1);
		String timestamp = dt.toString();
		signRequest(request, "/foo/bar/baz", username, secretKey, timestamp);
		AuthenticationFilter.matchHMACSHA1Signature(request, secretKey);
	}

	/**
	 * Adds signed headers to the request
	 */
	private static void signRequest(MockHttpServletRequest request, String requestURI, String reqUser, String reqKey, String timestamp) throws Exception {
		request.setRequestURI(requestURI);
		request.addHeader(AuthorizationConstants.USER_ID_HEADER, reqUser);
		request.addHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP, timestamp);
    	String signature = HMACUtils.generateHMACSHA1Signature(reqUser, 
    			request.getRequestURI(), 
    			request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP), 
    			reqKey);
		request.addHeader(AuthorizationConstants.SIGNATURE, signature);
	}
	
	@Test
	public void testFilter_validCredentials() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.USER_ID_HEADER)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BEARER_TOKEN_HEADER);
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockHttpRequest.getHeaders("Authorization")).thenReturn(Collections.enumeration(Collections.singletonList(BEARER_TOKEN_HEADER)));
		when(mockOidcManager.validateAccessToken(anyString())).thenReturn(""+userId);

		// by default the mocked oidcTokenHelper.validateJWT(bearerToken) won't throw any exception, so the token is deemed valid

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockOidcManager).validateAccessToken(BEARER_TOKEN);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		assertEquals(""+userId, requestCaptor.getValue().getParameter(AuthorizationConstants.USER_ID_PARAM));
		assertEquals("Bearer "+BEARER_TOKEN, requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME));
	}

	@Test
	public void noExternalUserIdParameter() throws Exception {
		Map<String, String[]> requestParams = new HashMap<String, String[]>();
		 // user is trying to 'sneak in' a validated userId
		requestParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] {"101010101"});
		when(mockHttpRequest.getParameterMap()).thenReturn(requestParams);
		when(mockHttpRequest.getParameter(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.USER_ID_HEADER)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BEARER_TOKEN_HEADER);
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockHttpRequest.getHeaders("Authorization")).thenReturn(Collections.enumeration(Collections.singletonList(BEARER_TOKEN_HEADER)));
		when(mockOidcManager.validateAccessToken(anyString())).thenReturn(""+userId);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
	
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		// Make sure the userId param has been removed
		assertEquals(""+userId, requestCaptor.getValue().getParameter(AuthorizationConstants.USER_ID_PARAM));
		
		
		
	}

	@Test
	public void testFilter_invalid_AccessToken() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.USER_ID_HEADER)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BEARER_TOKEN_HEADER);
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		OAuthErrorCode code = OAuthErrorCode.invalid_token;
		String description = "The token is invalid.";
		doThrow(new OAuthUnauthenticatedException(code, description)).when(mockOidcManager).validateAccessToken(BEARER_TOKEN);

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockOidcManager).validateAccessToken(BEARER_TOKEN);
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockPrintWriter).println("{\"reason\":\"" + code.name() + ". " + description + "\",\"error\":\"" + code.name() + "\",\"error_description\":\"" + description + "\"}");
	}

	@Test
	public void testFilter_no_Credentials() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.USER_ID_HEADER)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.SIGNATURE)).thenReturn(null);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockHttpRequest.getHeaders("Authorization")).thenReturn(Collections.emptyEnumeration());

		// method under test
		filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		verify(mockOidcManager, never()).validateAccessToken(BEARER_TOKEN);
		
		assertEquals("273950", requestCaptor.getValue().getParameter(AuthorizationConstants.USER_ID_PARAM));
		assertNull(requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME));
	}


}
