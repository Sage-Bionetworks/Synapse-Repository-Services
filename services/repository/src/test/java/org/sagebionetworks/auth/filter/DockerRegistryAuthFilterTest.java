package org.sagebionetworks.auth.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;

@ExtendWith(MockitoExtension.class)
public class DockerRegistryAuthFilterTest {

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Mock
	private FilterChain mockFilterChain;

	private DockerRegistryAuthFilter filter;

	private static final String USER = "user";
	private static final String PASS = "pass";

	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getDockerRegistryUser()).thenReturn(USER);
		when(mockConfig.getDockerRegistryPassword()).thenReturn(PASS);
		filter = new DockerRegistryAuthFilter(mockConfig);
	}
	
	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(), any());
	}

	@Test
	public void testDoFilterWithWrongUsernameAndPassword() throws Exception {
		String basicAuthenticationHeader = AuthorizationConstants.BASIC_PREFIX
				+ Base64.getEncoder().encodeToString("wrongRegistryUserName:wrongRegistryPassword".getBytes());
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(), any());
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		String basicAuthenticationHeader = AuthorizationConstants.BASIC_PREFIX
				+ Base64.getEncoder().encodeToString((USER+ ":" + PASS).getBytes());
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain).doFilter(any(HttpServletRequest.class), eq(mockResponse));
	}

}
