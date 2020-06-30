package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;

@ExtendWith(MockitoExtension.class)
public class CloudMailInAuthFilterTest {
	@Mock
	private Consumer mockConsumer;
	@Mock
	private PrintWriter mockPrintWriter;
	@Mock
	private StackConfiguration mockConfig;
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain;
	@Mock
	private Enumeration<String> mockEnumNames;
	
	private CloudMailInAuthFilter filter;

	private static final String AUTHORIZATION = "Authorization";
	
	private static final String USER = "user";
	private static final String PASS = "pass";
	
	// as set in StackConfiguration
	private static final String CORRECT_CREDENTIALS = USER + ":" + PASS;
	
	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getServiceAuthKey(StackConfiguration.SERVICE_CLOUDMAILIN)).thenReturn(USER);
		when(mockConfig.getServiceAuthSecret(StackConfiguration.SERVICE_CLOUDMAILIN)).thenReturn(PASS);
		
		filter = new CloudMailInAuthFilter(mockConfig, mockConsumer);
		
		assertTrue(filter.credentialsRequired());
		assertTrue(filter.reportBadCredentialsMetric());
	}
	
	@Test
	public void testAuthenticated() throws Exception {
		when(mockRequest.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				Base64.getEncoder().encodeToString(CORRECT_CREDENTIALS.getBytes()));
		
		when(mockRequest.getHeaderNames()).thenReturn(mockEnumNames);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		// to check that authorization proceeded, check that 'doFilter' was called and 'setStatus' was not
		ArgumentCaptor<HttpServletResponse> responseCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);
		verify(mockFilterChain).doFilter(any(), responseCaptor.capture());
		verify(responseCaptor.getValue(), never()).setStatus(eq(HttpStatus.SC_UNAUTHORIZED));
	}
	
	private void checkForUnauthorizedStatus(String message) {
		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(mockResponse).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_UNAUTHORIZED), captor.getValue());
		verify(mockPrintWriter).println("{\"reason\":\"" + message + "\"}");
	}

	@Test
	public void testNoCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);	
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		checkForUnauthorizedStatus("Missing required credentials in the authorization header.");
	}

	@Test
	public void testMissingBasicPrefix() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader(eq(AUTHORIZATION))).thenReturn("XXX "+
				Base64.getEncoder().encodeToString(CORRECT_CREDENTIALS.getBytes()));
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);	
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		checkForUnauthorizedStatus("Invalid Authorization header for basic authentication (Missing \\\"Basic \\\" prefix)");
	}

	@Test
	public void testMissingColon() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				Base64.getEncoder().encodeToString("NO-COLON-HERE".getBytes()));
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);	
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		checkForUnauthorizedStatus("Invalid Authorization header for basic authentication (Decoded credentials should be colon separated)");
	}

	@Test
	public void testWrongCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader(eq(AUTHORIZATION))).thenReturn("Basic "+
				Base64.getEncoder().encodeToString("foo:bar".getBytes()));
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);	
		verify(mockFilterChain, never()).doFilter(mockRequest, mockResponse);
		checkForUnauthorizedStatus("Invalid credentials.");
	}


}
