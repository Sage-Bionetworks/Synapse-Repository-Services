package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class AdminServiceAuthFilterTest {

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private Consumer mockConsumer;

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Mock
	private FilterChain mockFilterChain;

	@Mock
	private PrintWriter mockPrintWriter;

	@Mock
	private Enumeration<String> mockHeaderNames;

	private AdminServiceAuthFilter filter;

	private static final String KEY = "key";
	private static final String SECRET = "secret";

	@BeforeEach
	public void before() {
		when(mockConfig.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN)).thenReturn(KEY);
		when(mockConfig.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN)).thenReturn(SECRET);

		filter = new AdminServiceAuthFilter(mockConfig, mockConsumer);
	}

	@Test
	public void testCredentialsRequired() {
		assertTrue(filter.credentialsRequired());
	}

	@Test
	public void testIsAdminService() {
		assertTrue(filter.isAdminService());
	}

	@Test
	public void testDoFilterInternalWithEmptyCredentials() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		when(mockResponse.getWriter()).thenReturn(new PrintWriter(out));
		Optional<UserNameAndPassword> credentials = Optional.empty();

		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		assertTrue( out.toString(StandardCharsets.UTF_8.name()).contains(BasicAuthenticationFilter.MISSING_CREDENTIALS_MSG));
		verify(mockResponse).setStatus(HttpStatus.UNAUTHORIZED.value());

		verifyNoMoreInteractions(mockFilterChain);
		verifyNoMoreInteractions(mockRequest);
		verifyNoMoreInteractions(mockFilterChain);
	}

	@Test
	public void testDoFilterInternalWithCredentials() throws Exception {
		when(mockRequest.getHeaderNames()).thenReturn(mockHeaderNames);

		Optional<UserNameAndPassword> credentials = Optional.of(new UserNameAndPassword(KEY, SECRET));

		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);

		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		
		assertNotEquals(mockRequest, requestCaptor.getValue());
		verifyNoMoreInteractions(mockFilterChain);
		assertEquals(StackConfiguration.SERVICE_ADMIN, requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME));
	}
	@Test
	public void testDoFilterInternalWithBearerToken() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		when(mockResponse.getWriter()).thenReturn(new PrintWriter(out));
		when(mockRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn("Bearer xxxxx");

		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		assertTrue( out.toString(StandardCharsets.UTF_8.name()).contains(BasicAuthenticationFilter.MISSING_CREDENTIALS_MSG));
		verify(mockResponse).setStatus(HttpStatus.UNAUTHORIZED.value());

		verifyNoMoreInteractions(mockFilterChain);
		verifyNoMoreInteractions(mockRequest);
		verifyNoMoreInteractions(mockFilterChain);
	}
}
