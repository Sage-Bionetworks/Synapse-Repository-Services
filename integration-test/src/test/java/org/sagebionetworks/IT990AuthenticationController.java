package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;

public class IT990AuthenticationController {

	private static SynapseAdminClient adminSynapse;
	private static Long userToDelete;
	
	/**
	 * Signs in with username + password, has signed the ToU
	 */
	private static SynapseClient synapse;
	private static String email;
	private static String username;
	private static final String PASSWORD = "password";
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		// Don't use the SynapseClientHelper here, since we need something different
		email = UUID.randomUUID().toString() + "@sagebase.org";
		username = UUID.randomUUID().toString();
		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(PASSWORD);
		userToDelete = adminSynapse.createUser(nu);
		
		// Construct the client, but do nothing else
		synapse = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapse);
	}
	
	@Before
	public void setup() throws Exception {
		synapse.login(username, PASSWORD);
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void testLogin() throws Exception {
		synapse.login(username, PASSWORD);
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test(expected = SynapseUnauthorizedException.class)
	public void testLogin_BadCredentials() throws Exception {
		synapse.login(username, "incorrectPassword");
	}
	
	@Test
	public void testLogin_NoTermsOfUse() throws Exception {
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), false);
		Session session = synapse.login(username, PASSWORD);
		assertFalse(session.getAcceptsTermsOfUse());
		try {
			synapse.revalidateSession();
		} catch (SynapseTermsOfUseException e) { }
		
		// The session token can't be used to do much though
		try {
			synapse.getMyProfile();
			fail();
		} catch (SynapseForbiddenException e) { }
	}
	
	
	@Test
	public void testRevalidate() throws Exception {
		synapse.revalidateSession();
		assertNotNull(synapse.getCurrentSessionToken());
		
		synapse.getMyProfile();
	}
	
	@Test(expected=SynapseUnauthorizedException.class)
	public void testRevalidate_BadToken() throws Exception {
		synapse.setSessionToken("invalid-session-token");
		synapse.revalidateSession();
	}
	
	@Test
	public void testLoginThenLogout() throws Exception {
		synapse.logout();
		assertNull(synapse.getCurrentSessionToken());
	}
	
	@Test
	public void testCreateExistingUser() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(email);
		user.setUserName(UUID.randomUUID().toString());
		user.setFirstName("Foo");
		user.setLastName("Bar");
		
		try {
			synapse.createUser(user);
		} catch (SynapseServerException e) {
			assertTrue(e.getStatusCode()==409);
		}
	}
	
	@Test
	public void testChangePassword() throws Exception {
		String testNewPassword = "newPassword";
		synapse.changePassword(synapse.getCurrentSessionToken(), testNewPassword);
		
		// Session token should be invalid
		try {
			synapse.getMyProfile();
			fail();
		} catch (SynapseUnauthorizedException e) { }
		
		synapse.logout();
		synapse.login(username, testNewPassword);
		
		// Restore original password
		synapse.changePassword(synapse.getCurrentSessionToken(), PASSWORD);
	}
	
	@Test
	public void testChangePassword_NoToU() throws Exception {
		String testNewPassword = "newPassword";
		
		// Reject the terms
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), false);
		
		// Password change should still work
		synapse.changePassword(synapse.getCurrentSessionToken(), testNewPassword);
		synapse.logout();
		synapse.login(username, testNewPassword);
		
		// Restore original password
		synapse.changePassword(synapse.getCurrentSessionToken(), PASSWORD);
		
		// Accept the terms again (cleanup)
		synapse.login(username, PASSWORD);
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@Test
	public void testSignTermsViaSessionToken() throws Exception {
		String sessionToken = synapse.getCurrentSessionToken();
		
		// Reject the terms
		synapse.signTermsOfUse(sessionToken, false);
		
		// Now I can't do authenticated requests
		Session session = synapse.login(username, PASSWORD);
		assertFalse(session.getAcceptsTermsOfUse());
		
		// Accept the terms
		synapse.signTermsOfUse(sessionToken, true);
		
		session = synapse.login(username, PASSWORD);
		assertEquals(sessionToken, synapse.getCurrentSessionToken());
		assertTrue(session.getAcceptsTermsOfUse());
	}
	
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		// Note: non-production stacks do not send emails, but instead print a log message
		synapse.sendPasswordResetEmail(email);
	}
	
	
	@Test(expected = SynapseNotFoundException.class)
	public void testSendEmailInvalidUser() throws Exception {
		// There's no way a user like this exists :D
		synapse.sendPasswordResetEmail("invalid-user-name@sagebase.org" + UUID.randomUUID());
	}
	
	@Test
	public void testGetSecretKey() throws Exception {
		String apikey = synapse.retrieveApiKey();
		assertNotNull(apikey);
		System.out.println(apikey);
		
		// Use the API key
		synapse.logout();
		synapse.setUserName(username);
		synapse.setApiKey(apikey);
		
		// Should work
		synapse.getMyProfile();
		
		// This should make subsequent API key calls fail
		synapse.login(username, PASSWORD);
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), false);
		
		synapse.logout();
		synapse.setUserName(username);
		synapse.setApiKey(apikey);
		try {
			synapse.getMyProfile();
			fail();
		} catch (SynapseForbiddenException e) { }

		// Clean up
		synapse.login(username, PASSWORD);
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@Test
	public void testInvalidateSecretKey() throws Exception {
		String apikey = synapse.retrieveApiKey();
		synapse.invalidateApiKey();
		String secondKey = synapse.retrieveApiKey();
		
		// Should be different from the first one
		assertFalse(apikey.equals(secondKey));
	}

	@Test
	public void testGetOAuth2AuthenticationUrl() throws SynapseException{
		String rediect = "https://domain.com";
		OAuthUrlRequest request = new OAuthUrlRequest();
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl(rediect);
		OAuthUrlResponse response = synapse.getOAuth2AuthenticationUrl(request);
		assertNotNull(response);
		assertNotNull(response.getAuthorizationUrl());
	}
	
	/**
	 * Since a browser is need to get a real authentication code, we are just testing
	 * that everything is wires up correctly.
	 * @throws SynapseException 
	 */
	@Test
	public void testValidateOAuthAuthenticationCodeAndLogin() throws SynapseException {
		try {
			OAuthValidationRequest request = new OAuthValidationRequest();
			request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
			request.setRedirectUrl("https://www.synapse.org");
			// this invalid code will trigger a SynapseForbiddenException
			request.setAuthenticationCode("test auth code");
			synapse.validateOAuthAuthenticationCode(request);
			fail();
		} catch (SynapseForbiddenException e) {
			// OK
		}
	}
	
	/**
	 * Since a browser is need to get a real authentication code, we are just testing
	 * that everything is wires up correctly.
	 * @throws SynapseException 
	 */
	@Test
	public void testValidateOAuthAuthenticationCodeAndBindExternalId() throws SynapseException {
		try {
			OAuthValidationRequest request = new OAuthValidationRequest();
			request.setProvider(OAuthProvider.ORCID);
			// this invalid code will trigger a SynapseForbiddenException
			request.setAuthenticationCode("test auth code");
			synapse.bindOAuthProvidersUserId(request);
			fail();
		} catch (SynapseForbiddenException e) {
			// OK
		}
	}
}
