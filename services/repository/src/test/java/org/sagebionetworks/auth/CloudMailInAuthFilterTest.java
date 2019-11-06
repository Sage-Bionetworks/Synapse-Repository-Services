package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		// to check that authorization proceeded, check that 'doFilter' was called and 'setStatus' was not
		ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
		verify(filterChain).doFilter((HttpServletRequest)anyObject(), responseCaptor.capture());
		verify(responseCaptor.getValue(), never()).setStatus(eq(HttpStatus.SC_UNAUTHORIZED));
	}
	
	private void checkForUnauthorizedStatus() {
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
