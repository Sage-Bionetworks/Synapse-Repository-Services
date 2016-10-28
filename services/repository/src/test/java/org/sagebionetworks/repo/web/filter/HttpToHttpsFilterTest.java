package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.*;

import java.net.URL;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

import static org.mockito.Mockito.*;

public class HttpToHttpsFilterTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockFilterChain;
	
	HttpToHttpsRedirectFilter filter;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		filter = new HttpToHttpsRedirectFilter();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNoXForwardedProto() throws Exception {
		Mockito.when(mockRequest.getHeader(eq("X-Forwarded-Proto"))).thenReturn(null);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		Mockito.verify(mockFilterChain).doFilter(eq(mockRequest), eq(mockResponse));
		Mockito.verify(mockResponse, never()).sendRedirect(anyString());
	}

	@Test
	public void testHttpsXForwardedProto() throws Exception {
		Mockito.when(mockRequest.getHeader(eq("X-Forwarded-Proto"))).thenReturn("https");
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		Mockito.verify(mockFilterChain).doFilter(eq(mockRequest), eq(mockResponse));
		Mockito.verify(mockResponse, never()).sendRedirect(anyString());
	}
	
	@Test
	public void testHttpXForwardedProto() throws Exception {
		Mockito.when(mockRequest.getHeader(eq("X-Forwarded-Proto"))).thenReturn("http");
		StringBuffer expectedHttpURL = new StringBuffer("http://someHost/somePath");
		Mockito.when(mockRequest.getRequestURL()).thenReturn(expectedHttpURL);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		Mockito.verify(mockFilterChain, never()).doFilter(eq(mockRequest), eq(mockResponse));
		StringBuffer expectedHttpsURL = new StringBuffer("https://someHost/somePath");
		Mockito.verify(mockResponse).sendRedirect(eq(expectedHttpsURL.toString()));
	}

}
