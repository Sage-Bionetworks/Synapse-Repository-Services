package org.sagebionetworks.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableList;
import com.sun.syndication.io.impl.Base64; 

@ExtendWith(MockitoExtension.class)
class HttpAuthUtilTest {
	private static final String BEARER_TOKEN = "1a2b3c";

	@Mock
	private HttpServletRequest httpRequest;
	
	@Mock
	private HttpServletResponse httpResponse;
	
	@Mock
	private PrintWriter mockWriter;
	

	@Test
	void testGetBasicAuthenticationCredentials() {
		// test no authorization header
		when(httpRequest.getHeader("Authorization")).thenReturn(null);
		// method under test
		assertNull(HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));

		when(httpRequest.getHeader("Authorization")).thenReturn(" ");
		// method under test
		assertNull(HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));

		// not Basic Authentication
		when(httpRequest.getHeader("Authorization")).thenReturn("Bearer 1a2b3c");
		// method under test
		assertNull(HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
		
		// not properly formatted Basic auth
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic "+Base64.encode("some random text"));
		assertNull(HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));

		// test with proper Basic authorization header
		String username = "username";
		String password = "password";
		
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic "+Base64.encode(username+":"+password));
		UserNameAndPassword expected = new UserNameAndPassword(username, password);
		
		// method under test
		assertEquals(expected, HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));

		// extra white space
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic   \t"+Base64.encode(username+":"+password)+"\n\n    \t");
	
		// method under test
		assertEquals(expected, HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
	
	}

	@Test
	void testGetBearerToken() {
		// test no authorization header
		when(httpRequest.getHeader("Authorization")).thenReturn(null);
		// method under test
		assertNull(HttpAuthUtil.getBearerTokenFromStandardAuthorizationHeader(httpRequest));

		// proper bearer token
		when(httpRequest.getHeader("Authorization")).thenReturn("Bearer "+BEARER_TOKEN);
		// method under test
		assertEquals(BEARER_TOKEN, HttpAuthUtil.getBearerTokenFromStandardAuthorizationHeader(httpRequest));
}

	@Test
	void testGetBearerTokenFromAuthorizationHeader() {
		// method under test
		assertNull(HttpAuthUtil.getBearerTokenFromAuthorizationHeader(null));

		// method under test
		assertNull(HttpAuthUtil.getBearerTokenFromAuthorizationHeader(" "));

		// method under test
		assertNull(HttpAuthUtil.getBearerTokenFromAuthorizationHeader("Basic xxx"));
		
		// method under test
		assertEquals(BEARER_TOKEN, HttpAuthUtil.getBearerTokenFromAuthorizationHeader("Bearer "+BEARER_TOKEN));
		
		// what if there's extra white space?
		// method under test
		assertEquals(BEARER_TOKEN, HttpAuthUtil.getBearerTokenFromAuthorizationHeader("Bearer \t\t "+BEARER_TOKEN+"   "));
	}

	@Test
	void testSetBearerTokenHeader() {
		Map<String, String[]> headers = new HashMap<String, String[]>();
		HttpAuthUtil.setBearerTokenHeader(headers,  BEARER_TOKEN);
		assertEquals("Bearer "+BEARER_TOKEN, headers.get(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME)[0]);
	}

	@Test
	void testFilterAuthorizationHeaders() {
		String nonAuthHeader = "Accept";
		String nonAuthHeaderValue = "application/json";

		when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(ImmutableList.of(
				"Synapse-Authorization", "userId", "signatureTimestamp", "signature", "verifiedOAuthClientId", nonAuthHeader)));
		when(httpRequest.getHeaders(nonAuthHeader)).thenReturn(Collections.enumeration(Collections.singleton(nonAuthHeaderValue)));
		
		// method under test
		Map<String,String[]> actual = HttpAuthUtil.filterAuthorizationHeaders(httpRequest);
		
		assertEquals(1, actual.size());
		assertEquals(Collections.singleton(nonAuthHeader), actual.keySet());
		String[] values = actual.get(nonAuthHeader);
		assertEquals(1, values.length);
		assertEquals("application/json", values[0]);
	}
	
	@Test
	void testRejectWithStatus() throws Exception {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		
		when(httpResponse.getWriter()).thenReturn(mockWriter);
		
		// method under test
		HttpAuthUtil.reject(httpResponse, "bad request", status);
		
		verify(httpResponse).setStatus(status.value());
		verify(httpResponse).setContentType("application/json");
		verify(httpResponse).setHeader("WWW-Authenticate", "\"Digest\" your email");
		verify(mockWriter).println("{\"reason\":\"bad request\"}");
		
	}

	@Test
	void testRejectUnauthorized() throws Exception {
		when(httpResponse.getWriter()).thenReturn(mockWriter);
		
		// method under test
		HttpAuthUtil.reject(httpResponse, "missing token");
		
		verify(httpResponse).setStatus(401);
		verify(httpResponse).setContentType("application/json");
		verify(httpResponse).setHeader("WWW-Authenticate", "\"Digest\" your email");
		verify(mockWriter).println("{\"reason\":\"missing token\"}");
		
	}

}
