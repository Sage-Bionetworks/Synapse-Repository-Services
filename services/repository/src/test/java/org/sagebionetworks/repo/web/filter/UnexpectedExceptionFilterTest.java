package org.sagebionetworks.repo.web.filter;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class UnexpectedExceptionFilterTest {
	
	HttpServletRequest mockRequest;
	HttpServletResponse mockResponse;
	FilterChain mockChain;
	UnexpectedExceptionFilter filter;
	PrintWriter mockWriter;
	
	@Before
	public void before() throws IOException{
		mockRequest = Mockito.mock(HttpServletRequest.class);
		mockResponse = Mockito.mock(HttpServletResponse.class);
		mockChain = Mockito.mock(FilterChain.class);
		mockWriter = Mockito.mock(PrintWriter.class);
		filter = new UnexpectedExceptionFilter();
		when(mockResponse.getWriter()).thenReturn(mockWriter);
	}
	
	@Test
	public void testNoException() throws IOException, ServletException{
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// The response should not be changed
		verify(mockResponse, never()).setStatus(anyInt());
		verify(mockResponse, never()).getWriter();
	}

	/**
	 * We expect the server to recover from exceptions, so 503 is returned.
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public void testServletException() throws IOException, ServletException{
		ServletException error = new ServletException("Exception");
		doThrow(error).when(mockChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// The response should not be changed
		verify(mockResponse).setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
		verify(mockWriter).println("{\"reason\":\"Server error, try again later: Exception\"}");
	}
	
	/**
	 * We do not expect the server to recover from errors, so 500 is returned.
	 * @throws IOException
	 * @throws ServletException
	 */
	@Test
	public void testOutOfMemoryError() throws IOException, ServletException{
		OutOfMemoryError error = new OutOfMemoryError("Exception");
		doThrow(error).when(mockChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// The response should not be changed
		verify(mockResponse).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		verify(mockWriter).println(AuthorizationConstants.REASON_SERVER_ERROR);
	}
}
