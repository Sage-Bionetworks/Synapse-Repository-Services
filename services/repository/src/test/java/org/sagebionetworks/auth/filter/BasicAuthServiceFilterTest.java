package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;

@ExtendWith(MockitoExtension.class)
public class BasicAuthServiceFilterTest {
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private Consumer mockConsumer;
	
	@Mock
	private ServiceKeyAndSecretProvider mockKeyAndSecretProvider;
	
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
	
	private BasicAuthServiceFilter filter;
	
	private static final String SERVICE_NAME = "service";
	private static final String KEY = "key";
	private static final String SECRET = "secret";
	
	@BeforeEach
	public void before() {
		filter = Mockito.mock(BasicAuthServiceFilter.class, withSettings()
					.useConstructor(mockConfig, mockConsumer, mockKeyAndSecretProvider)
					.defaultAnswer(Answers.CALLS_REAL_METHODS)
		);
	}
	
	@Test
	public void testDoFilterInternalWithEmptyCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		Optional<UserNameAndPassword> credentials = Optional.empty();
		
		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		
		verify(filter).rejectRequest(mockResponse, "Missing required basic authentication credentials.");
		verifyZeroInteractions(mockFilterChain);
	}
	
	@Test
	public void testDoFilterInternalWithInvalidCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockKeyAndSecretProvider.validate(any(), any())).thenReturn(false);
		
		Optional<UserNameAndPassword> credentials = Optional.of(new UserNameAndPassword(KEY, SECRET));
		
		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		
		verify(mockKeyAndSecretProvider).validate(KEY, SECRET);
		verify(filter).rejectRequest(mockResponse, filter.getInvalidCredentialsMessage());		
		verifyZeroInteractions(mockFilterChain);
	}
	
	@Test
	public void testDoFilterInternalWithValidCredentials() throws Exception {
		when(mockRequest.getHeaderNames()).thenReturn(mockHeaderNames);
		when(mockKeyAndSecretProvider.getServiceName()).thenReturn(SERVICE_NAME);
		when(mockKeyAndSecretProvider.validate(any(), any())).thenReturn(true);
		
		Optional<UserNameAndPassword> credentials = Optional.of(new UserNameAndPassword(KEY, SECRET));
		
		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

		verify(mockKeyAndSecretProvider).validate(KEY, SECRET);
		verify(filter).isAdminService();
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		assertNotEquals(mockRequest, requestCaptor.getValue());
		assertTrue(requestCaptor.getValue().getParameterMap().isEmpty());
		assertEquals(SERVICE_NAME, requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME));
		
	}
	
	@Test
	public void testDoFilterInternalWithAdminService() throws Exception {
		when(mockRequest.getHeaderNames()).thenReturn(mockHeaderNames);
		when(mockKeyAndSecretProvider.getServiceName()).thenReturn(SERVICE_NAME);
		when(mockKeyAndSecretProvider.validate(any(), any())).thenReturn(true);
		when(filter.isAdminService()).thenReturn(true);
		
		Optional<UserNameAndPassword> credentials = Optional.of(new UserNameAndPassword(KEY, SECRET));
		
		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		
		verify(mockKeyAndSecretProvider).validate(KEY, SECRET);
		verify(filter).isAdminService();
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));	
		assertNotEquals(mockRequest, requestCaptor.getValue());
		
		assertEquals(SERVICE_NAME, requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME));
		
		String userId = requestCaptor.getValue().getParameter(AuthorizationConstants.USER_ID_PARAM);
		
		assertEquals(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString(), userId);
	}
	
	@Test
	public void testDoFilterInternalWithInjectedHeader() throws Exception {
		
		Enumeration<String> injectedHeaderNames = Collections.enumeration(Arrays.asList(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME));
		
		when(mockRequest.getHeaderNames()).thenReturn(injectedHeaderNames);
		when(mockKeyAndSecretProvider.getServiceName()).thenReturn(SERVICE_NAME);
		when(mockKeyAndSecretProvider.validate(any(), any())).thenReturn(true);
		
		Optional<UserNameAndPassword> credentials = Optional.of(new UserNameAndPassword(KEY, SECRET));
		
		// Call under test
		filter.validateCredentialsAndDoFilterInternal(mockRequest, mockResponse, mockFilterChain, credentials);
		
		// Makes sure that if the header is there in the request, the value is not taken from the original request
		verify(mockRequest, times(0)).getHeaders(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME);
		
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));	
		
		assertNotEquals(mockRequest, requestCaptor.getValue());
		
		assertEquals(SERVICE_NAME, requestCaptor.getValue().getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME));
	}

}
