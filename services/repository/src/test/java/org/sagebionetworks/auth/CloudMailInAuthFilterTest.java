package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class CloudMailInAuthFilterTest {
	private CloudMailInAuthFilter filter;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain filterChain;
	
	private static final String AUTHORIZATION = "Authorization";
	
	// as set in StackConfiguration
	private static final String CORRECT_CREDENTIALS = "user:pass";

	@Before
	public void setUp() throws Exception {
		filter = new CloudMailInAuthFilter();
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);
		filterChain = Mockito.mock(FilterChain.class);
	}
	
	@Test
	public void testAuthenticated() throws Exception {
		when(request.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				new String(Base64.encodeBase64(CORRECT_CREDENTIALS.getBytes())));
		filter.doFilter(request, response, filterChain);
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(filterChain).doFilter(requestCaptor.capture(), (HttpServletResponse)anyObject());
		HttpServletRequest modifiedRequest = requestCaptor.getValue();
		assertEquals("text/plain", modifiedRequest.getHeader("Accept"));
		{
			Enumeration<String> headers = modifiedRequest.getHeaders("Accept");
			boolean foundit = false;
			int acceptHeaderCount = 0;
			while (headers.hasMoreElements()) {
				acceptHeaderCount++;
				if ("text/plain".equals(headers.nextElement())) foundit=true;
			}
			assertEquals(1, acceptHeaderCount);
			assertTrue(foundit);
		}
		{
			Enumeration<String> headerNames = modifiedRequest.getHeaderNames();
			boolean foundit = false;
			int acceptHeaderNameCount = 0;
			while (headerNames.hasMoreElements()) {
				acceptHeaderNameCount++;
				String headerName = headerNames.nextElement();
				if ("Accept".equals(headerName)) foundit=true;
			}
			assertEquals(1, acceptHeaderNameCount);
			assertTrue(foundit);
		}
	}
	
	private  void checkForUnauthorizedStatus() {
		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(response).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_UNAUTHORIZED), captor.getValue());		
	}

	@Test
	public void testNoCredentials() throws Exception {
		filter.doFilter(request, response, filterChain);	
		verify(filterChain, never()).doFilter(request, response);
		checkForUnauthorizedStatus();
	}

	@Test
	public void testMissingBasicPrefix() throws Exception {
		when(request.getHeader(eq(AUTHORIZATION))).thenReturn("XXX "+
				new String(Base64.encodeBase64(CORRECT_CREDENTIALS.getBytes())));
		filter.doFilter(request, response, filterChain);	
		verify(filterChain, never()).doFilter(request, response);
		checkForUnauthorizedStatus();
	}

	@Test
	public void testMissingColon() throws Exception {
		when(request.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				new String(Base64.encodeBase64("NO-COLON-HERE".getBytes())));
		filter.doFilter(request, response, filterChain);	
		verify(filterChain, never()).doFilter(request, response);
		checkForUnauthorizedStatus();
	}

	@Test
	public void testWrongCredentials() throws Exception {
		when(request.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				new String(Base64.encodeBase64("foo:bar".getBytes())));
		filter.doFilter(request, response, filterChain);	
		verify(filterChain, never()).doFilter(request, response);
		checkForUnauthorizedStatus();
	}


}
