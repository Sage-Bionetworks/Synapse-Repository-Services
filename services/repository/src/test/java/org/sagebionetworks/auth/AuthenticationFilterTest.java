package org.sagebionetworks.auth;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.google.gwt.i18n.shared.DateTimeFormatInfo;
import com.google.gwt.i18n.shared.impl.cldr.DateTimeFormatInfoImpl_en;

public class AuthenticationFilterTest {
	
	private AuthenticationFilter filter;
	
	private AuthenticationService mockAuthService;
	private static final String sessionToken = "someSortaSessionToken";
	private static final String secretKey = "Totally a plain text key :D";
	private static final String username = "AuthFilter@test.sagebase.org";
	private static final String userId = "123456789";

	@Before
	public void setupFilter() throws Exception {
		mockAuthService = Mockito.mock(AuthenticationService.class);
		Mockito.when(mockAuthService.revalidate(Mockito.eq(sessionToken))).thenReturn(userId);
		Mockito.when(mockAuthService.getUsername(Mockito.eq(userId))).thenReturn(username);
		Mockito.when(mockAuthService.getSecretKey(Mockito.eq(username))).thenReturn(secretKey);
		
		final Map<String,String> filterParams = new HashMap<String, String>();
		filterParams.put("allow-anonymous", "true");

		filter = new AuthenticationFilter(mockAuthService);
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
		Assert.assertNotNull(modRequest);
		String anonymous = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, anonymous);
	}
	
	@Test
	public void testSessionToken_asHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);
		
		// Session token should be recognized
		Mockito.verify(mockAuthService, Mockito.times(1)).revalidate(Mockito.eq(sessionToken));
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String sessionUsername = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(username, sessionUsername);
	}
	
	@Test
	public void testSessionToken_asParameter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);

		// Session token should be recognized
		Mockito.verify(mockAuthService, Mockito.times(1)).revalidate(Mockito.eq(sessionToken));
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String sessionUsername = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(username, sessionUsername);
	}

	/**
	 * Since DateUtils cannot be used on the server side, here is a workaround. 
	 * Code taken from: 
	 * https://code.google.com/p/google-web-toolkit/issues/detail?id=1176
	 */
	private static com.google.gwt.i18n.shared.DateTimeFormat createGwtDTFormat(
			String pattern) throws Exception {
		Constructor<com.google.gwt.i18n.shared.DateTimeFormat> c = com.google.gwt.i18n.shared.DateTimeFormat.class
				.getDeclaredConstructor(String.class, DateTimeFormatInfo.class);
		c.setAccessible(true);
		return c.newInstance(pattern, new DateTimeFormatInfoImpl_en());
	}
	
	@Test
	public void testHmac() throws Exception {
		com.google.gwt.i18n.shared.DateTimeFormat dateFormat = createGwtDTFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("FooBar");
		request.addHeader(AuthorizationConstants.USER_ID_HEADER, username);
		request.addHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP, dateFormat.format(new Date()));
    	String signature = HMACUtils.generateHMACSHA1Signature(username, 
    			request.getRequestURI(), 
    			request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP), 
    			secretKey);
		request.addHeader(AuthorizationConstants.SIGNATURE, signature);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);

		// Signature should match
		Mockito.verify(mockAuthService, Mockito.times(1)).getSecretKey(Mockito.eq(username));
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String passedAlongUsername = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(username, passedAlongUsername);
	}

}
