package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import javax.servlet.FilterChain;
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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OAuthClientManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;

@ExtendWith(MockitoExtension.class)
public class OAuthClientAuthFilterTest {
	
	@Mock
	private OAuthClientManager mockOauthClientManager;
	
	@Mock
	private HttpServletRequest mockHttpRequest;
	
	@Mock
	private HttpServletResponse mockHttpResponse;
	
	@Mock
	private PrintWriter mockPrintWriter;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private Consumer mockConsumer;
	
	@InjectMocks
	private OAuthClientAuthFilter oAuthClientAuthFilter;
	
	@Captor
	private ArgumentCaptor<HttpServletRequest> requestCaptor;
	
	private static final String CLIENT_ID = "oauthClientId";
	private static final String CLIENT_SECRET = "secret";
	private static final String BASIC_HEADER = "Basic "+ Base64.getEncoder().encodeToString((CLIENT_ID+":"+CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

	@BeforeEach
	public void setUp() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(BASIC_HEADER);
		assertTrue(oAuthClientAuthFilter.credentialsRequired());
		assertTrue(oAuthClientAuthFilter.reportBadCredentialsMetric());
	}

	@Test
	public void testFilter_validCredentials_client_secret_basic() throws Exception {
		when(mockOauthClientManager.validateClientCredentials((OAuthClientIdAndSecret)any())).thenReturn(true);
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.enumeration(
				Collections.singletonList(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)));
		when(mockHttpRequest.getHeaders(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(
				Collections.enumeration(Collections.singletonList(BASIC_HEADER)));

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockOauthClientManager).validateClientCredentials(any());
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());
		
		assertEquals(CLIENT_ID, requestCaptor.getValue().getHeader(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER));
	}

	@Test
	public void testFilter_validCredentials_client_secret_post() throws Exception {
		when(mockOauthClientManager.validateClientCredentials((OAuthClientIdAndSecret)any())).thenReturn(true);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		when(mockHttpRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
		when(mockHttpRequest.getParameter("client_secret")).thenReturn(CLIENT_SECRET);
		when(mockHttpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);

		ArgumentCaptor<OAuthClientIdAndSecret> credentialsCaptor = ArgumentCaptor.forClass(OAuthClientIdAndSecret.class);
		verify(mockOauthClientManager).validateClientCredentials(credentialsCaptor.capture());
		assertEquals(CLIENT_ID, credentialsCaptor.getValue().getClient_id());
		assertEquals(CLIENT_SECRET, credentialsCaptor.getValue().getClient_secret());
		verify(mockFilterChain).doFilter(requestCaptor.capture(), (ServletResponse)any());

		assertEquals(CLIENT_ID, requestCaptor.getValue().getHeader(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER));
	}

	@Test
	public void testFilter_validCredentials_client_secret_post_query_params() throws Exception {
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		when(mockHttpRequest.getParameter("client_id")).thenReturn(CLIENT_ID);
		when(mockHttpRequest.getParameter("client_secret")).thenReturn(CLIENT_SECRET);
		when(mockHttpRequest.getQueryString()).thenReturn("client_id="+CLIENT_ID+"&client_secret="+CLIENT_SECRET);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockPrintWriter).println("{\"reason\":\"Client credentials must not be passed as query parameters.\"}");
	}

	@Test
	public void testFilter_invalid_Credentials() throws Exception {
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);
		when(mockOauthClientManager.validateClientCredentials((OAuthClientIdAndSecret)any())).thenReturn(false);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockOauthClientManager).validateClientCredentials(any());

		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockPrintWriter).println("{\"reason\":\"OAuth Client ID and secret must be passed via Basic Authentication. Credentials are missing or invalid.\"}");
	}

	@Test
	public void testFilter_no_Credentials() throws Exception {
		when(mockHttpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME)).thenReturn(null);
		when(mockHttpResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		oAuthClientAuthFilter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
		
		verify(mockOauthClientManager, never()).validateClientCredentials(any());

		verify(mockFilterChain, never()).doFilter((ServletRequest)any(), (ServletResponse)any());
		verify(mockHttpResponse).setStatus(401);
		verify(mockHttpResponse).setContentType("application/json");
		verify(mockPrintWriter).println("{\"reason\":\"Missing required credentials in the authorization header.\"}");
	}

}
