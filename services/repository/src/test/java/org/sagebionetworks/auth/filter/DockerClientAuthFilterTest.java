package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Base64;

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
import org.sagebionetworks.auth.services.AuthenticationService;
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

	@InjectMocks
	private DockerClientAuthFilter filter;
	
	private String header;
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final Long USERID = 123L;

	@BeforeEach
	public void before() {
		header = AuthorizationConstants.BASIC_PREFIX + Base64.getEncoder().encodeToString((USERNAME+":"+PASSWORD).getBytes());
		assertFalse(filter.credentialsRequired());
		assertFalse(filter.reportBadCredentialsMetric());
	}

	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService, never()).login(any(LoginRequest.class));
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
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
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.login(loginCred))
				.thenThrow(new UnauthenticatedException("Wrong credentials"));

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
		verify(mockPrintWriter).println("{\"reason\":\"Credentials are missing or invalid.\"}");
	}

	@Test
	public void testDoFilterWithNotFoundUsername() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.lookupUserForAuthentication(USERNAME))
				.thenThrow(new NotFoundException());

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService).lookupUserForAuthentication(USERNAME);
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
		verify(mockPrintWriter).println("{\"reason\":\"Credentials are missing or invalid.\"}");
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.lookupUserForAuthentication(USERNAME))
				.thenReturn(mockPrincipalAlias);
		when(mockPrincipalAlias.getPrincipalId()).thenReturn(USERID);

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService).lookupUserForAuthentication(anyString());
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		HttpServletRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(request.getParameter(AuthorizationConstants.USER_ID_PARAM), USERID.toString());
	}

}
