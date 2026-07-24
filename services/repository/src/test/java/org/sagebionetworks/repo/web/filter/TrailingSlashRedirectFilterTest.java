package org.sagebionetworks.repo.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TrailingSlashRedirectFilterTest {

	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain;

	private TrailingSlashRedirectFilter filter;

	@BeforeEach
	public void setUp() {
		filter = new TrailingSlashRedirectFilter();
	}

	@Test
	public void testNoTrailingSlash() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(eq(mockRequest), eq(mockResponse));
		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testRootPath() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(eq(mockRequest), eq(mockResponse));
		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testSingleTrailingSlash() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version/");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		// Verify the filter chain was called with a wrapped request
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		// Verify the wrapped request returns the normalized URI
		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		// Verify no redirect was sent
		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testMultipleTrailingSlashes() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version///");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testQueryStringPreservation() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/entity/syn123/");
		when(mockRequest.getQueryString()).thenReturn("includeEntity=true&format=json");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/entity/syn123", wrappedRequest.getRequestURI());
		assertEquals("includeEntity=true&format=json", wrappedRequest.getQueryString());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testQueryStringEmpty() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version/");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testPOSTWithTrailingSlash() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/entity/");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/entity", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testTrailingSlashWithWhitespace() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version/ ");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testTrailingWhitespaceOnly() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version  ");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

	@Test
	public void testMixedTrailingSlashesAndWhitespace() throws Exception {
		when(mockRequest.getRequestURI()).thenReturn("/repo/v1/version// \t");

		// call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));

		HttpServletRequest wrappedRequest = requestCaptor.getValue();
		assertEquals("/repo/v1/version", wrappedRequest.getRequestURI());

		verify(mockResponse, never()).setStatus(308);
		verify(mockResponse, never()).setHeader(anyString(), anyString());
	}

}
