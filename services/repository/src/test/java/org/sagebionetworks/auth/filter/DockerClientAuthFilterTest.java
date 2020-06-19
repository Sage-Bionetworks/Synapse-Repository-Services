package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)

public class DockerClientAuthFilterTest {
	
	@Mock
	private PrintWriter mockPrintWriter;
	@Mock
	private AuthenticationService mockAuthenticationService;
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain;
	@Mock
	private PrincipalAlias mockPrincipalAlias;
	
	@Mock 
	private OIDCTokenHelper mockOidcTokenHelper;
	
	@Mock 
	private OpenIDConnectManager mockOidcManager;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private Consumer mockConsumer;
	
	@InjectMocks
	private DockerClientAuthFilter filter;
	
	private String header;
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final Long USERID = 123L;
	private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	private static final List<String> HEADER_NAMES = Collections.singletonList(AUTHORIZATION_HEADER_NAME);


	@BeforeEach
	public void before() {
		header = AuthorizationConstants.BASIC_PREFIX + Base64.getEncoder().encodeToString((USERNAME+":"+PASSWORD).getBytes());
		assertFalse(filter.credentialsRequired());
		assertTrue(filter.reportBadCredentialsMetric());
	}

	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockRequest.getHeaders(AUTHORIZATION_HEADER_NAME)).thenReturn(Collections.emptyEnumeration());
				
		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthenticationService, never()).login(any(LoginRequest.class));
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
		verify(mockOidcManager, never()).validateAccessToken(anyString());
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		HttpServletRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(request.getParameter(AuthorizationConstants.USER_ID_PARAM),
				BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
	}

	@Test
	public void testDoFilterWithWrongUsernameAndPassword() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader(AUTHORIZATION_HEADER_NAME)).thenReturn(header);
		when(mockOidcManager.validateAccessToken(PASSWORD)).thenThrow(new IllegalArgumentException()); // not an access token
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.login(loginCred))
				.thenThrow(new UnauthenticatedException("Wrong credentials"));

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockOidcManager).validateAccessToken(PASSWORD);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
		verify(mockPrintWriter).println("{\"reason\":\"Invalid credentials.\"}");
	}

	@Test
	public void testDoFilterWithNotFoundUsername() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader(AUTHORIZATION_HEADER_NAME)).thenReturn(header);
		when(mockOidcManager.validateAccessToken(PASSWORD)).thenThrow(new IllegalArgumentException()); // not an access token
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.lookupUserForAuthentication(USERNAME))
				.thenThrow(new NotFoundException());

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockOidcManager).validateAccessToken(PASSWORD);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService).lookupUserForAuthentication(USERNAME);
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
		verify(mockPrintWriter).println("{\"reason\":\"Invalid credentials.\"}");
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		when(mockRequest.getHeader(AUTHORIZATION_HEADER_NAME)).thenReturn(header);
		when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockRequest.getHeaders(AUTHORIZATION_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(header)));
		when(mockOidcManager.validateAccessToken(PASSWORD)).thenThrow(new IllegalArgumentException()); // not an access token
		String token = "token";
		when(mockOidcTokenHelper.createTotalAccessToken(USERID)).thenReturn(token);
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.lookupUserForAuthentication(USERNAME))
				.thenReturn(mockPrincipalAlias);
		when(mockPrincipalAlias.getPrincipalId()).thenReturn(USERID);

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockOidcManager).validateAccessToken(PASSWORD);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService).lookupUserForAuthentication(anyString());
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		HttpServletRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(request.getParameter(AuthorizationConstants.USER_ID_PARAM), USERID.toString());
		assertEquals("Bearer "+token, request.getHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME));
	}
	
	@Test
	public void testDoFilterWithUsernameAndAccessToken() throws Exception {
		when(mockRequest.getHeader(AUTHORIZATION_HEADER_NAME)).thenReturn(header);
		when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(HEADER_NAMES));
		when(mockRequest.getHeaders(AUTHORIZATION_HEADER_NAME)).thenReturn(Collections.enumeration(Collections.singleton(header)));
		when(mockOidcManager.validateAccessToken(PASSWORD)).thenReturn(USERID.toString());

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthenticationService, never()).login(any());
		verify(mockOidcManager).validateAccessToken(PASSWORD);
		
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		HttpServletRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(request.getParameter(AuthorizationConstants.USER_ID_PARAM), USERID.toString());
		assertEquals("Bearer "+PASSWORD, request.getHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME));
	}


}
