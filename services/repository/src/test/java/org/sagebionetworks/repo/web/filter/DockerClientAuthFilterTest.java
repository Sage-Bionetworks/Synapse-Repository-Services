package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.auth.BasicAuthUtils;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.syndication.io.impl.Base64;

public class DockerClientAuthFilterTest {
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

	private DockerClientAuthFilter filter;
	private String header;
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final Long USERID = 123L;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		filter = new DockerClientAuthFilter();
		header = BasicAuthUtils.BASIC_PREFIX + Base64.encode(USERNAME+":"+PASSWORD);
		ReflectionTestUtils.setField(filter, "authenticationService", mockAuthenticationService);
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
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginRequest loginCred = new LoginRequest();
		loginCred.setUsername(USERNAME);
		loginCred.setPassword(PASSWORD);
		when(mockAuthenticationService.login(loginCred))
				.thenThrow(new NotFoundException());

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).login(loginCred);
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
	}

	@Test
	public void testDoFilterWithNotFoundUsername() throws Exception {
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
