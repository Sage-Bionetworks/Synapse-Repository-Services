package org.sagebionetworks.repo.web.filter;

import static org.mockito.Mockito.verify;
import static org.sagebionetworks.repo.web.filter.HSTSFilter.MAX_AGE;
import static org.sagebionetworks.repo.web.filter.HSTSFilter.MAX_AGE_SECONDS;
import static org.sagebionetworks.repo.web.filter.HSTSFilter.STRICT_TRANSPORT_SECURITY;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HSTSFilterTest {
	private HSTSFilter filter;
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		filter = new HSTSFilter();
	}
	
	@Test
	public void testHeaderAdded() throws Exception {
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verify(mockResponse).setHeader(STRICT_TRANSPORT_SECURITY, MAX_AGE + MAX_AGE_SECONDS);
	}
}
