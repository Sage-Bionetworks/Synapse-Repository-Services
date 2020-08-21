package org.sagebionetworks.repo.web.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.NoHandlerFoundException;

@RunWith(MockitoJUnitRunner.class)
public class UnexpectedExceptionFilterTest {

	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockChain;
	@Mock
	PrintWriter mockWriter;

	UnexpectedExceptionFilter filter;


	@Before
	public void before() throws IOException{
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
		verify(mockResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
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
