package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;

@ExtendWith(MockitoExtension.class)
public class OAuthAccessTokenFilterTest {
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private ServletOutputStream mockServletOutputStream;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	OIDCTokenHelper oidcTokenHelper;
	
	@InjectMocks
	private OAuthAccessTokenFilter oauthAccessTokenFilter;
	
	@Captor
	private ArgumentCaptor<HttpServletRequest> requestCaptor;
	
	@BeforeEach
	public void setUp() throws Exception {
	}
	
	private static final String BEARER_TOKEN = "bearer token";
	private static final String BEARER_TOKEN_HEADER = "Bearer "+BEARER_TOKEN;

	@Test
	public void testFilter_validCredentials() throws Exception {
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.enumeration(
				Collections.singletonList(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)));
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BEARER_TOKEN_HEADER);
		
		// by default the mocked oidcTokenHelper.validateJWT(bearerToken) won't throw any exception, so the token is deemed valid

		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(oidcTokenHelper).validateJWT(BEARER_TOKEN);
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		assertEquals(BEARER_TOKEN_HEADER, requestCaptor.getValue().getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME));
	}

	@Test
	public void testFilter_invalid_AccessToken() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BEARER_TOKEN_HEADER);

		doThrow(new IllegalArgumentException()).when(oidcTokenHelper).validateJWT(BEARER_TOKEN);

		when(mockHttpResponse.getOutputStream()).thenReturn(mockServletOutputStream);

		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(oidcTokenHelper).validateJWT(BEARER_TOKEN);
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid access token\"}");
	}

	@Test
	public void testFilter_no_Credentials() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		when(mockHttpResponse.getOutputStream()).thenReturn(mockServletOutputStream);

		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(oidcTokenHelper, never()).validateJWT(BEARER_TOKEN);
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid access token\"}");
	}

}
