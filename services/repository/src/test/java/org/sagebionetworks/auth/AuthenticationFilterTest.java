package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthenticationFilterTest {

	@InjectMocks
	private AuthenticationFilter filter;

	@Mock
	private AuthenticationService mockAuthService;
	@Mock
	private UserManager mockUserManager;

	private static final String sessionToken = "someSortaSessionToken";
	private static final String secretKey = "Totally a plain text key :D";
	private static final String username = "AuthFilter@test.sagebase.org";
	private static final Long userId = 123456789L;

	@BeforeEach
	public void setupFilter() throws Exception {
		when(mockAuthService.revalidate(eq(sessionToken), eq(false))).thenReturn(userId);
		when(mockAuthService.getSecretKey(eq(userId))).thenReturn(secretKey);
		when(mockAuthService.hasUserAcceptedTermsOfUse(eq(userId))).thenReturn(true);
		PrincipalAlias pa = new PrincipalAlias();
		pa.setPrincipalId(userId);
		when(mockUserManager.lookupUserByUsernameOrEmail(eq(username))).thenReturn(pa);

		final Map<String,String> filterParams = new HashMap<String, String>();
		filterParams.put("allow-anonymous", "true");

		filter.init(new FilterConfig() {
			public String getFilterName() { 
				return ""; 
			}
			
			public String getInitParameter(String name) {
				return filterParams.get(name);
			}
			
			public Enumeration<String> getInitParameterNames() {
				Set<String> keys = filterParams.keySet();
				final Iterator<String> i = keys.iterator();
				return new Enumeration<String>() {
					public boolean hasMoreElements() {
						return i.hasNext();
					}
					public String nextElement() {
						return i.next();
					}
				};
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
		when(mockAuthService.revalidate(eq(""), eq(false))).thenThrow(new UnauthorizedException("That is not a valid session token"));
		
		filter.doFilter(request, response, filterChain);
		
		// Should default to anonymous
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String anonymous = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertTrue(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString().equals(anonymous));
	}
	
	@Test
	public void testSessionToken_asHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);
		
		// Session token should be recognized
		Mockito.verify(mockAuthService, times(1)).revalidate(eq(sessionToken), eq(false));
		ServletRequest modRequest = filterChain.getRequest();
		assertNotNull(modRequest);
		String sessionUserId = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		assertEquals(userId.toString(), sessionUserId);
	}
	
	@Test
	public void testSessionToken_asParameter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
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
		MockHttpServletRequest request = new MockHttpServletRequest();
		String timestamp = new DateTime().toString();
		signRequest(request, "/foo/bar/baz", username, secretKey, timestamp);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
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
}
