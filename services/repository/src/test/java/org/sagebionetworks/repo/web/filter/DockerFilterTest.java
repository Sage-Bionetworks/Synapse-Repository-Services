package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.syndication.io.impl.Base64;

public class DockerFilterTest {
	@Mock
	AuthenticationService mockAuthenticationService;
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	FilterChain mockFilterChain;
	@Mock
	PrincipalAlias mockPrincipalAlias;

	DockerFilter filter;
	String header;
	String username = "username";
	String password = "password";
	Long userId = 123L;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		filter = new DockerFilter();
		header = BasicAuthUtils.BASIC_PREFIX + Base64.encode(username+":"+password);
		ReflectionTestUtils.setField(filter, "authenticationService", mockAuthenticationService);
	}

	@Test
	public void testDoFilterWithoutBasicAuth() throws Exception {
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService, never()).authenticate(any(LoginCredentials.class), any(DomainType.class));
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
		LoginCredentials loginCred = new LoginCredentials();
		loginCred.setEmail(username);
		loginCred.setPassword(password);
		when(mockAuthenticationService.authenticate(loginCred , DomainType.SYNAPSE))
				.thenThrow(new NotFoundException());

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).authenticate(loginCred , DomainType.SYNAPSE);
		verify(mockAuthenticationService, never()).lookupUserForAuthentication(anyString());
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
	}

	@Test
	public void testDoFilterWithNotFoundUsername() throws Exception {
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginCredentials loginCred = new LoginCredentials();
		loginCred.setEmail(username);
		loginCred.setPassword(password);
		when(mockAuthenticationService.lookupUserForAuthentication(username))
				.thenThrow(new NotFoundException());

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).authenticate(loginCred , DomainType.SYNAPSE);
		verify(mockAuthenticationService).lookupUserForAuthentication(username);
		verify(mockFilterChain, never()).doFilter(any(HttpServletRequest.class), eq(mockResponse));
	}

	@Test
	public void testDoFilterAuthenticateSuccess() throws Exception {
		when(mockRequest.getHeader("Authorization")).thenReturn(header);
		LoginCredentials loginCred = new LoginCredentials();
		loginCred.setEmail(username);
		loginCred.setPassword(password);
		when(mockAuthenticationService.lookupUserForAuthentication(username))
				.thenReturn(mockPrincipalAlias);
		when(mockPrincipalAlias.getPrincipalId()).thenReturn(userId);

		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		verify(mockAuthenticationService).authenticate(loginCred , DomainType.SYNAPSE);
		verify(mockAuthenticationService).lookupUserForAuthentication(anyString());
		ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), eq(mockResponse));
		HttpServletRequest request = requestCaptor.getValue();
		assertNotNull(request);
		assertEquals(request.getParameter(AuthorizationConstants.USER_ID_PARAM), userId.toString());
	}

}
