package org.sagebionetworks.auth;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.gwt.client.schema.adapter.DateUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * These tests will require the UserManager to be pointed towards RDS rather than Crowd
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthenticationFilterTest {
	
	@Autowired
	private AuthenticationService authenticationService;
	
	private AuthenticationFilter filter;
	private String sessionToken;

	@BeforeClass
	public void setupFilter() throws Exception {
		final Map<String,String> filterParams = new HashMap<String, String>();
		filterParams.put("allow-anonymous", "true");

		filter = new AuthenticationFilter();
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
	
	@Before
	public void setup() throws Exception {
		sessionToken = authenticationService.getSessionTokenFromUserName(AuthorizationConstants.TEST_USER_NAME);
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
		String username = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(AuthorizationConstants.ANONYMOUS_USER_ID, username);
	}
	
	@Test
	public void testSessionToken_asHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);
		
		// Session token should be recognized
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String username = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(AuthorizationConstants.TEST_USER_NAME, username);
	}
	
	@Test
	public void testSessionToken_asParameter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);

		// Session token should be recognized
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String username = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(AuthorizationConstants.TEST_USER_NAME, username);
	}
	
	@Test
	public void testHmac() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("FooBar");
		request.addHeader(AuthorizationConstants.USER_ID_HEADER, AuthorizationConstants.TEST_USER_NAME);
		request.addHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP, DateUtils.convertDateToString(FORMAT.DATE_TIME, new Date()));
    	String signature = HMACUtils.generateHMACSHA1Signature(AuthorizationConstants.TEST_USER_NAME, 
    			request.getRequestURI(), 
    			request.getHeader(AuthorizationConstants.SIGNATURE_TIMESTAMP), 
    			authenticationService.getSecretKey(AuthorizationConstants.TEST_USER_NAME));
		request.addHeader(AuthorizationConstants.SIGNATURE, signature);
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		
		filter.doFilter(request, response, filterChain);

		// Signature should match
		ServletRequest modRequest = filterChain.getRequest();
		Assert.assertNotNull(modRequest);
		String username = modRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		Assert.assertEquals(AuthorizationConstants.TEST_USER_NAME, username);
	}

}
