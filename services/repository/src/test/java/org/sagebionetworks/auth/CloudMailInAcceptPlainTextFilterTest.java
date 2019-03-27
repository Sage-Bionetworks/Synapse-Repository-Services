package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.verify;

import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class CloudMailInAcceptPlainTextFilterTest {
	private CloudMailInAcceptPlainTextFilter filter;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain filterChain;


	@Before
	public void setUp() throws Exception {
		filter = new CloudMailInAcceptPlainTextFilter();
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);
		filterChain = Mockito.mock(FilterChain.class);
	}
	
	@Test
	public void testHeaderAdded() throws Exception {
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		filter.doFilter(request, response, filterChain);
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
	


}
