package org.sagebionetworks.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
class HttpAuthUtilTest {
	private static final String BEARER_TOKEN = "1a2b3c";

	@Mock
	private HttpServletRequest httpRequest;
	
	@Mock
	private HttpServletResponse httpResponse;
	
	@Mock
	private PrintWriter mockWriter;
	
	private static final String username = "username";
	private static final String password = "password";
	
	private static String base64Encode(String in) {
		return Base64.getEncoder().encodeToString(in.getBytes());
	}

	@Test
	void testUsesBasicAuthenticationCredentials() {
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic placeholder");
		// method under test
		assertTrue(HttpAuthUtil.usesBasicAuthentication(httpRequest));
	}

	@Test
	void testUsesBasicAuthenticationCredentials_NoHeader() {
		when(httpRequest.getHeader("Authorization")).thenReturn(null);
		// method under test
		assertFalse(HttpAuthUtil.usesBasicAuthentication(httpRequest));
	}

	@Test
	void testUsesBasicAuthenticationCredentials_EmptyHeader() {
		when(httpRequest.getHeader("Authorization")).thenReturn("");
		// method under test
		assertFalse(HttpAuthUtil.usesBasicAuthentication(httpRequest));
	}

	@Test
	void testUsesBasicAuthenticationCredentials_NotBasic() {
		when(httpRequest.getHeader("Authorization")).thenReturn("Bearer placeholder");
		// method under test
		assertFalse(HttpAuthUtil.usesBasicAuthentication(httpRequest));
	}

	@Test
	void testGetBasicAuthenticationCredentialsWithNoHeader() {
		// test no authorization header
		when(httpRequest.getHeader("Authorization")).thenReturn(null);
		// method under test
		assertEquals(Optional.empty(), HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
	}
	
	@Test
	void testGetBasicAuthenticationCredentialsWithEmptyHeader() {
		// Empty header
		when(httpRequest.getHeader("Authorization")).thenReturn(" ");
		// method under test
		assertEquals(Optional.empty(), HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
	}
	
	@Test
	void testGetBasicAuthenticationCredentialsWithWrongHeader() {

		// not Basic Authentication
		when(httpRequest.getHeader("Authorization")).thenReturn("Bearer 1a2b3c");
		// method under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		});
		
		assertEquals("Invalid Authorization header for basic authentication (Missing \"Basic \" prefix)", ex.getMessage());
	}
	
	@Test
	void testGetBasicAuthenticationCredentialsWithoutColon() {
		// not properly formatted Basic auth
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic "+base64Encode("some random text"));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		});
		
		assertEquals("Invalid Authorization header for basic authentication (Decoded credentials should be colon separated)", ex.getMessage());
		
	}

	@Test
	void testGetBasicAuthenticationCredentialsWithCredentials() {

		when(httpRequest.getHeader("Authorization")).thenReturn("Basic "+base64Encode(username+":"+password));
		
		Optional<UserNameAndPassword> expected = Optional.of(new UserNameAndPassword(username, password));
		
		// method under test
		assertEquals(expected, HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
	}

	@Test
	void testGetBasicAuthenticationCredentialsWithExtraSpace() {
		// extra white space
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic   \t"+base64Encode(username+":"+password)+"\n\n    \t");
	
		Optional<UserNameAndPassword> expected = Optional.of(new UserNameAndPassword(username, password));
		
		// method under test
		assertEquals(expected, HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest));
		
	}
	
	@Test
	void testGetBasicAuthenticationCredentialsWithInvalidEncoding() {
		// invalid encoding
		when(httpRequest.getHeader("Authorization")).thenReturn("Basic "+base64Encode(username+":"+password)+"__");
		
		// method under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		});
		
		assertEquals("Invalid Authorization header for basic authentication (Malformed Base64 encoding: Input byte array has incorrect ending byte at 24)", ex.getMessage());
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
	void testSetServiceNameHeader() {
		Map<String, String[]> headers = new HashMap<String, String[]>();
		
		String serviceName = "someService";
		
		// Call under test
		HttpAuthUtil.setServiceNameHeader(headers,  serviceName);
		
		assertEquals(serviceName, headers.get(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME)[0]);
	}

	@Test
	void testFilterAuthorizationHeaders() {
		String nonAuthHeader = "Accept";
		String nonAuthHeaderValue = "application/json";

		when(httpRequest.getHeaderNames()).thenReturn(Collections.enumeration(ImmutableList.of(
				"Synapse-Authorization", "sessionToken", "userId", "signatureTimestamp", "signature", "verifiedOAuthClientId", nonAuthHeader)));
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
		HttpStatus status = HttpStatus.UNAUTHORIZED;

		when(httpResponse.getWriter()).thenReturn(mockWriter);

		// method under test
		HttpAuthUtil.rejectWithErrorResponse(httpResponse, "bad request", status);

		verify(httpResponse).setStatus(status.value());
		verify(httpResponse).setContentType("application/json");
		verify(httpResponse).setHeader("WWW-Authenticate", "\"Digest\" your email");
		verify(mockWriter).println("{\"reason\":\"bad request\"}");
	}

	@Test
	void testNoWwwAuthenticateOnReject_nonUnauthorized() throws Exception {
		HttpStatus status = HttpStatus.FORBIDDEN;

		when(httpResponse.getWriter()).thenReturn(mockWriter);

		// method under test
		HttpAuthUtil.rejectWithErrorResponse(httpResponse, "bad request", status);

		verify(httpResponse).setStatus(status.value());
		verify(httpResponse).setContentType("application/json");
		verify(httpResponse, never()).setHeader("WWW-Authenticate", "\"Digest\" your email");
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
