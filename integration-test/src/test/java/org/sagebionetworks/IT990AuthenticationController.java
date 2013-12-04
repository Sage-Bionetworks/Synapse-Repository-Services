package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;

public class IT990AuthenticationController {
	private static SynapseClient synapse;
	
	private static final String username = StackConfiguration.getIntegrationTestUserThreeName();
	private static final String password = StackConfiguration.getIntegrationTestUserThreePassword();

	@BeforeClass
	public static void beforeClass() throws Exception {
		String authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		String repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new SynapseClientImpl();
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
	}
	
	@Before
	public void setup() throws Exception {
		synapse.login(username, password);
	}

	@Test
	public void testLogin() throws Exception {
		synapse.login(username, password);
		assertNotNull(synapse.getCurrentSessionToken());
	}
	
	@Test(expected = SynapseUnauthorizedException.class)
	public void testLogin_BadCredentials() throws Exception {
		synapse.login(username, "incorrectPassword");
	}
	
	@Test
	public void testLogin_NoTermsOfUse() throws Exception {
		String username = StackConfiguration.getIntegrationTestRejectTermsOfUseName();
		String password = StackConfiguration.getIntegrationTestRejectTermsOfUsePassword();
		Session session = synapse.login(username, password);
		assertFalse(session.getAcceptsTermsOfUse());
		try {
			synapse.revalidateSession();
		} catch (SynapseTermsOfUseException e) { }
	}
	
	@Test
	public void testLogin_IgnoreTermsOfUse() throws Exception {
		String username = StackConfiguration.getIntegrationTestRejectTermsOfUseName();
		String password = StackConfiguration.getIntegrationTestRejectTermsOfUsePassword();
		synapse.login(username, password);
		
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
		user.setEmail(username);
		user.setFirstName("dev");
		user.setLastName("usr");
		user.setDisplayName("dev usr");
		
		try {
			synapse.createUser(user);
		} catch (SynapseUserException e) {
			assertTrue(e.getMessage().contains("409"));
		}
	}
	
	@Test
	public void testCreateUser_AcceptToU() throws Exception {	
		String username = "integration@test." + UUID.randomUUID();
		String password = "password";
		
		NewUser user = new NewUser();
		user.setEmail(username);
		user.setFirstName("foo");
		user.setLastName("bar");
		user.setDisplayName("foo bar");
		// Note: passwords are only accepted in this request in non-production stacks
		user.setPassword(password);
		
		synapse.createUser(user);
		
		// Login and fail an authenticated request
		synapse.login(username, password);
		try {
			synapse.getMyProfile();
			fail();
		} catch (SynapseForbiddenException e) { }
		
		// Now accept the terms and try an authenticated request
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
		synapse.getMyProfile();
	}
	
	@Test
	public void testChangePassword() throws Exception {
		String testNewPassword = "newPassword";
		synapse.changePassword(synapse.getCurrentSessionToken(), testNewPassword);
		synapse.logout();
		synapse.login(username, testNewPassword);
		
		// Restore original password
		synapse.changePassword(synapse.getCurrentSessionToken(), password);
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
		synapse.changePassword(synapse.getCurrentSessionToken(), password);
		
		// Accept the terms again (cleanup)
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@Test
	public void testSignTermsViaSessionToken() throws Exception {
		String sessionToken = synapse.getCurrentSessionToken();
		
		// Reject the terms
		synapse.signTermsOfUse(sessionToken, false);
		
		// Now I can't do authenticated requests
		Session session = synapse.login(username, password);
		assertFalse(session.getAcceptsTermsOfUse());
		
		// Accept the terms
		synapse.signTermsOfUse(sessionToken, true);
		
		session = synapse.login(username, password);
		assertEquals(sessionToken, synapse.getCurrentSessionToken());
		assertTrue(session.getAcceptsTermsOfUse());
	}
	
	@Test
	public void testSendResetPasswordEmail() throws Exception {
		// Note: non-production stacks do not send emails, but instead print a log message
		synapse.sendPasswordResetEmail(username);
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
		
		// Use the API key
		synapse.logout();
		synapse.setUserName(username);
		synapse.setApiKey(apikey);
		
		// Should work
		synapse.getMyProfile();
		
		// This should make subsequent API key calls fail
		synapse.login(username, password);
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), false);
		
		synapse.logout();
		synapse.setUserName(username);
		synapse.setApiKey(apikey);
		try {
			synapse.getMyProfile();
			fail();
		} catch (SynapseForbiddenException e) { }

		// Clean up
		synapse.login(username, password);
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
	
	/**
	 * Since we don't know Google's private OpenID information, this is a bit difficult to integration test
	 * At best, this test makes sure the service is wired up
	 */
	@Test
	public void testOpenIDCallback() throws Exception {
		try {
			synapse.passThroughOpenIDParameters("org.sagebionetworks.openid.provider=GOOGLE");
			fail();
		} catch (SynapseUnauthorizedException e) {
			assertTrue(e.getMessage().contains("Required parameter missing"));
		}
	}
}
