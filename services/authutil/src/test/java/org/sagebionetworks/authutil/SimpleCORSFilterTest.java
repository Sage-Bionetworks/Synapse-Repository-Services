package org.sagebionetworks.authutil;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class SimpleCORSFilterTest {
	
	HttpServletRequest mockRequest;
	HttpServletResponse mockResponse;
	FilterChain mockChain;
	SimpleCORSFilter filter;
	PrintWriter mockWriter;
	
	@Before
	public void before() throws IOException{
		mockRequest = Mockito.mock(HttpServletRequest.class);
		mockResponse = Mockito.mock(HttpServletResponse.class);
		mockChain = Mockito.mock(FilterChain.class);
		mockWriter = Mockito.mock(PrintWriter.class);
		filter = new SimpleCORSFilter();
		when(mockResponse.getWriter()).thenReturn(mockWriter);
		when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(new ArrayList<String>()));
	}
	
	@Test
	public void testNotPreflight() throws IOException, ServletException{
		filter.doFilter(mockRequest, mockResponse, mockChain);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN, SimpleCORSFilter.ALL_ORIGINS);
		verifyNoMoreInteractions(mockResponse);
		verify(mockChain).doFilter(mockRequest, mockResponse);
	}


	@Test
	public void testIsPreflight() throws IOException, ServletException{
		// request header "access control request method" is set.
		when(mockRequest.getHeader(SimpleCORSFilter.ACCESS_CONTROL_REQUEST_METHOD)).thenReturn("set");
		// verify filter echos back custom headers that we request. 
		String testHeaders = "Origin, X-Requested-With, Content-Type, Accept, " + AuthorizationConstants.SESSION_TOKEN_PARAM;
		when(mockRequest.getHeader(SimpleCORSFilter.ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn(testHeaders);
		when(mockRequest.getMethod()).thenReturn(SimpleCORSFilter.OPTIONS);
		filter.doFilter(mockRequest, mockResponse, mockChain);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_ALLOW_ORIGIN, SimpleCORSFilter.ALL_ORIGINS);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_MAX_AGE, SimpleCORSFilter.MAX_AGE);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_ALLOW_HEADERS, testHeaders);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_ALLOW_METHODS, SimpleCORSFilter.METHODS);
		verify(mockResponse).addHeader(SimpleCORSFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
		
		verify(mockChain, never()).doFilter(mockRequest, mockResponse);
	}
	

}
