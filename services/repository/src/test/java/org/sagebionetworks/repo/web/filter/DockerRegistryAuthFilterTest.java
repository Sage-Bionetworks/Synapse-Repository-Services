package org.sagebionetworks.repo.web.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.auth.BasicAuthUtils;

import com.sun.syndication.io.impl.Base64;

public class DockerRegistryAuthFilterTest {
	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Mock
	private FilterChain mockFilterChain;

	private DockerRegistryAuthFilter filter;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		filter = new DockerRegistryAuthFilter();
	}

	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), (HttpServletResponse)anyObject());
	}

	@Test
	public void testDoFilterWithWrongUsernameAndPassword() throws Exception {
		String basicAuthenticationHeader = BasicAuthUtils.BASIC_PREFIX + Base64.encode(
				"wrongRegistryUserName:wrongRegistryPassword");
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), (HttpServletResponse)anyObject());
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		String basicAuthenticationHeader = BasicAuthUtils.BASIC_PREFIX + Base64.encode(
				StackConfigurationSingleton.singleton().getDockerRegistryUser()+":"+
						StackConfigurationSingleton.singleton().getDockerRegistryPassword());
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain).doFilter(any(HttpServletRequest.class), eq(mockResponse));
	}

}
