package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudMailInAcceptPlainTextFilterTest {
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@InjectMocks
	private CloudMailInAcceptPlainTextFilter filter;
	
	@Test
	public void testHeaderAdded() throws Exception {
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), any());
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
	


}
