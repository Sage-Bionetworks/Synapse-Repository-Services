package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.model.AuthorizationConstants;

@ExtendWith(MockitoExtension.class)
public class DockerRegistryAuthFilterTest {
	
	@Mock
	private Consumer mockConsumer;
	
	@Mock
	private PrintWriter mockPrintWriter;
	
	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Mock
	private FilterChain mockFilterChain;

	@Mock
	private Enumeration<String> mockEnumNames;

	private DockerRegistryAuthFilter filter;

	private static final String USER = "user";
	private static final String PASS = "pass";

	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getServiceAuthKey(StackConfiguration.SERVICE_DOCKER_REGISTRY)).thenReturn(USER);
		when(mockConfig.getServiceAuthSecret(StackConfiguration.SERVICE_DOCKER_REGISTRY)).thenReturn(PASS);
		
		filter = new DockerRegistryAuthFilter(mockConfig, mockConsumer);
		
		assertTrue(filter.credentialsRequired());
		assertTrue(filter.reportBadCredentialsMetric());
	}
	
	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(), any());
		verify(mockPrintWriter).println("{\"reason\":\"Missing required credentials in the authorization header.\"}");
	}

	@Test
	public void testDoFilterWithWrongUsernameAndPassword() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		String basicAuthenticationHeader = AuthorizationConstants.BASIC_PREFIX
				+ Base64.getEncoder().encodeToString("wrongRegistryUserName:wrongRegistryPassword".getBytes());
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockFilterChain, never()).doFilter(any(), any());
		verify(mockPrintWriter).println("{\"reason\":\"Invalid credentials.\"}");
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		String basicAuthenticationHeader = AuthorizationConstants.BASIC_PREFIX
				+ Base64.getEncoder().encodeToString((USER+ ":" + PASS).getBytes());
		when(mockRequest.getHeader("Authorization")).thenReturn(basicAuthenticationHeader);
		when(mockRequest.getHeaderNames()).thenReturn(mockEnumNames);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(any(), eq(mockResponse));
	}

}
