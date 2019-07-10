package org.sagebionetworks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.oauth.OAuthAccountCreationRequest;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.OAuthUrlRequest;
import org.sagebionetworks.repo.model.oauth.OAuthUrlResponse;
import org.sagebionetworks.repo.model.oauth.OAuthValidationRequest;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.util.SerializationUtils;

public class IT990AuthenticationController {

	private static SynapseAdminClient adminSynapse;
	private static Long userToDelete;
	
	/**
	 * Signs in with username + password, has signed the ToU
	 */
	private static SynapseClient synapse;
	private static String email;
	private static String emailAlias;
	private static String username;
	private static final String PASSWORD = "password"+UUID.randomUUID().toString();
	private static String receipt = null;
	private static final String SYNAPSE_ENDPOINT = "https://www.synapse.org/";
	private static String emailS3Key, emailAliasS3Key;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		// Don't use the SynapseClientHelper here, since we need something different
		email = UUID.randomUUID().toString() + "@sagebase.org";
		emailS3Key = EmailValidationUtil.getBucketKeyForEmail(email);
		username = UUID.randomUUID().toString();
		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(PASSWORD);
		
		userToDelete = adminSynapse.createUser(nu);
		
		// Construct the client, but do nothing else
		synapse = new SynapseClientImpl();
		
		SynapseClientHelper.setEndpoints(synapse);

		performLogin();
		
		// Add an alternative email as an alias
		emailAlias = UUID.randomUUID().toString() + "@foo.com";
		emailAliasS3Key = EmailValidationUtil.getBucketKeyForEmail(emailAlias);
		assertFalse(EmailValidationUtil.doesFileExist(emailAliasS3Key, 2000L));
		String endpoint = "https://www.synapse.org?";
		synapse.additionalEmailValidation(Long.parseLong(synapse.getMyProfile().getOwnerId()), emailAlias, endpoint);

		// Complete the email addition
		String encodedToken = EmailValidationUtil.getTokenFromFile(emailAliasS3Key, "href=\"" + endpoint, "\">");
		EmailValidationSignedToken token = SerializationUtils.hexDecodeAndDeserialize(encodedToken, EmailValidationSignedToken.class);
		// we are _not_ setting it to be the notification email
		synapse.addEmail(token, false);
		// Let us delete this so we can test later on for new emails
		EmailValidationUtil.deleteFile(emailAliasS3Key);
	}
	
	private static void performLogin() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(PASSWORD);
		request.setAuthenticationReceipt(receipt);
		receipt = synapse.login(request).getAuthenticationReceipt();
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@Before
	public void setup() throws Exception {
		performLogin();
	}
	
	@After
	public void after() throws Exception {
		// Cleanup to make sure we receive the latest email
		EmailValidationUtil.deleteFile(emailS3Key);
		EmailValidationUtil.deleteFile(emailAliasS3Key);
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void testLoginWithReceipt() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(PASSWORD);
		LoginResponse response = synapse.login(request);
		assertNotNull(response);
		assertNotNull(response.getAuthenticationReceipt());
	}

	@Test
	public void testLoginThenLogout() throws Exception {
		synapse.logout();
		assertNull(synapse.getCurrentSessionToken());
	}

	@Test
	public void testChangePasswordWithOldPassword() throws Exception {
		String testNewPassword = "newPassword"+UUID.randomUUID();
		synapse.changePassword(username, PASSWORD, testNewPassword, null);
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(testNewPassword);
		synapse.login(request);

		//change password back
		synapse.changePassword(username, testNewPassword, PASSWORD,null);
	}

	@Test
	public void testSignTermsViaSessionToken() throws Exception {
		String sessionToken = synapse.getCurrentSessionToken();
		
		// Reject the terms
		synapse.signTermsOfUse(sessionToken, false);
	}

	@Test
	public void testNewSendResetPasswordEmail() throws Exception {
		synapse.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, email);
		
		assertTrue(EmailValidationUtil.doesFileExist(emailS3Key, 2000L));
		
		String emailContent = EmailValidationUtil.readFile(emailS3Key);

		assertTrue(emailContent.contains("Reset Synapse Password"));
	}
	
	@Test
	public void testSendResetPasswordEmailWithEmailAlias() throws Exception {
		synapse.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, emailAlias);

		assertTrue(EmailValidationUtil.doesFileExist(emailAliasS3Key, 2000L));

		String emailContent = EmailValidationUtil.readFile(emailAliasS3Key);

		assertTrue(emailContent.contains("Reset Synapse Password"));
	}
	
	@Test
	public void testSendResetPasswordEmailToWrongAlias() throws Exception {
		String missingAlias = UUID.randomUUID().toString() + "@synapse.org";
		String missingAliasS3Key = EmailValidationUtil.getBucketKeyForEmail(missingAlias);

		synapse.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, missingAlias);

		assertFalse(EmailValidationUtil.doesFileExist(missingAliasS3Key, 2000L));
	}
	
	@Test
	public void testGetSecretKey() throws Exception {
		String apikey = synapse.retrieveApiKey();
		assertNotNull(apikey);
		System.out.println(apikey);
		
		// Use the API key
		synapse.logout();
		synapse.setUsername(username);
		synapse.setApiKey(apikey);
		
		// Should work
		synapse.getMyProfile();
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
	public void testCreateAccountViaOAuth2() throws SynapseException {
		try {
			OAuthAccountCreationRequest request = new OAuthAccountCreationRequest();
			request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
			request.setRedirectUrl("https://www.synapse.org");
			// this invalid code will trigger a SynapseForbiddenException
			request.setAuthenticationCode("test auth code");
			request.setUserName("uname");
			synapse.createAccountViaOAuth2(request);
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
	
	/**
	 * Since a browser is need to get a real authentication code, we are just testing
	 * that everything is wires up correctly.
	 * @throws SynapseException 
	 */
	@Test(expected=SynapseNotFoundException.class)
	public void testUnbindExternalId() throws SynapseException {
		synapse.unbindOAuthProvidersUserId(OAuthProvider.ORCID, "http://orcid.org/1234-5678-9876-5432");
	}

}
