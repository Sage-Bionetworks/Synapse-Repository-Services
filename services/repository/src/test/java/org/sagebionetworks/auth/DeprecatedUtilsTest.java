package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;

public class DeprecatedUtilsTest {
	
	LoginResponse response;
	LoginCredentials credentials;
	
	@Before
	public void before() {
		response = new LoginResponse();
		response.setAcceptsTermsOfUse(true);
		response.setSessionToken("sessionToken");
		
		credentials = new LoginCredentials();
		credentials.setEmail("email@foo.com");
		credentials.setPassword("secret");
	}

	@Test
	public void testCreateSession() {
		// call under test
		Session session = DeprecatedUtils.createSession(response);
		assertNotNull(session);
		assertEquals(response.getSessionToken(), session.getSessionToken());
		assertEquals(response.getAcceptsTermsOfUse(), session.getAcceptsTermsOfUse());
	}
	
	@Test
	public void testCreateSessionTOUfalse() {
		response.setAcceptsTermsOfUse(false);
		// call under test
		Session session = DeprecatedUtils.createSession(response);
		assertNotNull(session);
		assertEquals(response.getSessionToken(), session.getSessionToken());
		assertEquals(response.getAcceptsTermsOfUse(), session.getAcceptsTermsOfUse());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateSessionNull() {
		response = null;
		// call under test
		DeprecatedUtils.createSession(response);
	}
	
	@Test
	public void testCreateLoginRequest() {
		// Call under test
		LoginRequest loginRequest = DeprecatedUtils.createLoginRequest(credentials);
		assertNotNull(loginRequest);
		assertEquals(null, loginRequest.getAuthenticationReceipt());
		assertEquals(credentials.getEmail(), loginRequest.getUsername());
		assertEquals(credentials.getPassword(), loginRequest.getPassword());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateLoginRequestNull() {
		credentials = null;
		// Call under test
		DeprecatedUtils.createLoginRequest(credentials);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateLoginRequestNullEmail() {
		credentials.setEmail(null);
		// Call under test
		DeprecatedUtils.createLoginRequest(credentials);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateLoginRequestNullPassord() {
		credentials.setPassword(null);
		// Call under test
		DeprecatedUtils.createLoginRequest(credentials);
	}
}
