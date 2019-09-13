package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.securitytools.PBKDF2Utils;

import com.sun.jersey.core.util.Base64;

@RunWith(MockitoJUnitRunner.class)
public class OAuthClientAuthFilterTest {
	
	@Mock
	private OAuthClientDao mockOauthClientDao;
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private ServletOutputStream mockServletOutputStream;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@InjectMocks
	private OAuthClientAuthFilter oAuthClientAuthFilter;
	
	@Captor
	private ArgumentCaptor<HttpServletRequest> requestCaptor;
	
	private static final String CLIENT_ID = "oauthClientId";
	private static final String CLIENT_SECRET = "clientPassword";

	@Before
	public void setUp() throws Exception {
		String secretHash = PBKDF2Utils.hashPassword(CLIENT_SECRET, null);
		byte[] clientSalt = PBKDF2Utils.extractSalt(secretHash);
		when(mockOauthClientDao.getSecretSalt(CLIENT_ID)).thenReturn(clientSalt);
		when(mockOauthClientDao.checkOAuthClientSecretHash(CLIENT_ID, secretHash)).thenReturn(true);
		when(mockHttpResponse.getOutputStream()).thenReturn(mockServletOutputStream);
	}

	@Test
	public void testFilter_validCredentials() throws Exception {
		String basicHeader = "Basic "+new String(Base64.encode(CLIENT_ID+":"+CLIENT_SECRET));
		
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(basicHeader);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		assertEquals(CLIENT_ID, requestCaptor.getValue().getParameter(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_PARAM));
	}

	@Test
	public void testFilter_invalid_Credentials() throws Exception {
		String basicHeader = "Basic "+new String(Base64.encode(CLIENT_ID+":incorrect-secret"));

		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(basicHeader);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid OAuth 2.0 client credentials\"}");
	}

	@Test
	public void testFilter_no_Credentials() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockServletOutputStream).println("{\"reason\":\"Missing or invalid OAuth 2.0 client credentials\"}");
	}

}
