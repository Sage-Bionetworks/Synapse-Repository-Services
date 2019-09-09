package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;

import com.sun.jersey.core.util.Base64;

@RunWith(MockitoJUnitRunner.class)
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
	
	@Before
	public void setUp() throws Exception {
		String bearerTokenHeader = "Bearer "+BEARER_TOKEN;
		
		when(mockHttpRequest.getHeader(HttpAuthUtil.AUTHORIZATION_HEADER_NAME)).thenReturn(bearerTokenHeader);
		
		when(mockHttpResponse.getOutputStream()).thenReturn(mockServletOutputStream);
	}
	
	private static final String BEARER_TOKEN = "bearer token";

	@Test
	public void testFilter_validCredentials() throws Exception {
		
		// by default the mocked oidcTokenHelper.validateJWT(bearerToken) won't throw any exception, so the token is deemed valid

		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		assertEquals(BEARER_TOKEN, requestCaptor.getValue().getParameter(AuthorizationConstants.OAUTH_VERIFIED_ACCESS_TOKEN));
	}

	@Test
	public void testFilter_invalid_AccessToken() throws Exception {
		doThrow(new IllegalArgumentException()).when(oidcTokenHelper).validateJWT(BEARER_TOKEN);


		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid access token\"}");
	}

	@Test
	public void testFilter_no_Credentials() throws Exception {
		when(mockHttpRequest.getHeader(HttpAuthUtil.AUTHORIZATION_HEADER_NAME)).thenReturn(null);

		// method under test
		oauthAccessTokenFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid access token\"}");
	}

}
