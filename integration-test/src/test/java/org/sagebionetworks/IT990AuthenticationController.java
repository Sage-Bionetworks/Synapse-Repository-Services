package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.auth.AuthenticatedOn;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
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

@ExtendWith(ITTestExtension.class)
public class IT990AuthenticationController {
	
	private static SynapseClient synapseClient;
	private static Long clientUserToDelete;
	
	private static String email;
	private static String emailAlias;
	private static String username;
	private static String password = "password"+UUID.randomUUID().toString();
	private static String receipt = null;
	private static final String SYNAPSE_ENDPOINT = "https://www.synapse.org/";
	private static String emailS3Key, emailAliasS3Key;
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		// Don't use the base class test user here, since we need something different
		email = UUID.randomUUID().toString() + "@sagebase.org";
		emailS3Key = EmailValidationUtil.getBucketKeyForEmail(email);
		username = UUID.randomUUID().toString();
		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(password);

		LoginResponse loginResponse = adminSynapse.createIntegrationTestUser(nu);
		String accessTokenSubject = JSONWebTokenHelper.getSubjectFromJWTAccessToken(loginResponse.getAccessToken());
		clientUserToDelete = Long.parseLong(accessTokenSubject);
		
		// Construct the client, but do nothing else
		synapseClient = new SynapseClientImpl();
		
		SynapseClientHelper.setEndpoints(synapseClient);

		performLogin();
		
		// Add an alternative email as an alias
		emailAlias = UUID.randomUUID().toString() + "@foo.com";
		emailAliasS3Key = EmailValidationUtil.getBucketKeyForEmail(emailAlias);
		assertFalse(EmailValidationUtil.doesFileExist(emailAliasS3Key, 2000L));
		String endpoint = "https://www.synapse.org?";
		synapseClient.additionalEmailValidation(Long.parseLong(synapseClient.getMyProfile().getOwnerId()), emailAlias, endpoint);

		// Complete the email addition
		String encodedToken = EmailValidationUtil.getTokenFromFile(emailAliasS3Key, "href=\"" + endpoint, "\">");
		EmailValidationSignedToken token = SerializationUtils.hexDecodeAndDeserialize(encodedToken, EmailValidationSignedToken.class);
		// we are _not_ setting it to be the notification email
		synapseClient.addEmail(token, false);
		// Let us delete this so we can test later on for new emails
		EmailValidationUtil.deleteFile(emailAliasS3Key);
	}
	
	private static void performLogin() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		request.setAuthenticationReceipt(receipt);
		receipt = synapseClient.loginForAccessToken(request).getAuthenticationReceipt();
		synapseClient.signTermsOfUse(synapseClient.getAccessToken());
	}
	
	@BeforeEach
	public void setup() throws Exception {
		performLogin();
	}
	
	@AfterEach
	public void after() throws Exception {
		// Cleanup to make sure we receive the latest email
		EmailValidationUtil.deleteFile(emailS3Key);
		EmailValidationUtil.deleteFile(emailAliasS3Key);
	}
	
	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		adminSynapse.deleteUser(clientUserToDelete);
	}
	
	@Test
	public void testGetAuthenticatedOn() throws Exception {
		AuthenticatedOn authenticatedOn = synapseClient.getAuthenticatedOn();
		assertNotNull(authenticatedOn.getAuthenticatedOn());
	}
	
	@Test
	public void testLoginForAccessToken() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		LoginResponse response = synapseClient.loginForAccessToken(request);
		assertNotNull(response);
		assertNotNull(response.getAccessToken());
		assertNotNull(response.getAuthenticationReceipt());		
	}

	@Test
	public void testLoginWithReceipt() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		LoginResponse response = synapseClient.login(request);
		assertNotNull(response);
		assertNotNull(response.getAuthenticationReceipt());
	}

	@Test
	public void testLoginThenLogout() throws Exception {
		synapseClient.deleteSessionTokenHeader();
		assertNull(synapseClient.getCurrentSessionToken());
	}

	@Test
	public void testChangePasswordWithOldPassword() throws Exception {
		String testNewPassword = "newPassword"+UUID.randomUUID();
		synapseClient.changePassword(username, password, testNewPassword, null);
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(testNewPassword);
		synapseClient.login(request);

		password = testNewPassword;
		
		// try to change password back, since we changed it recently this won't be allowed (See https://sagebionetworks.jira.com/browse/PLFM-8464)
		assertThrows(SynapseBadRequestException.class, () -> {			
			synapseClient.changePassword(username, testNewPassword, password,null);
		});
	}

	@Test
	public void testSignTermsViaSessionToken() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setUsername(username);
		request.setPassword(password);
		request.setAuthenticationReceipt(receipt);
		synapseClient.login(request);
		String sessionToken = synapseClient.getCurrentSessionToken();
		// Accept the terms
		synapseClient.signTermsOfUse(sessionToken);
		// Reject the terms
		synapseClient.signTermsOfUse(sessionToken);
	}

	@Test
	public void testSignTermsViaAccessToken() throws Exception {
		String accessToken = synapseClient.getAccessToken();
		synapseClient.signTermsOfUse(accessToken);
	}

	@Test
	public void testNewSendResetPasswordEmail() throws Exception {
		synapseClient.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, email);
		
		assertTrue(EmailValidationUtil.doesFileExist(emailS3Key, 2000L));
		
		String emailContent = EmailValidationUtil.readFile(emailS3Key);

		assertTrue(emailContent.contains("Reset Synapse Password"));
	}
	
	@Test
	public void testSendResetPasswordEmailWithEmailAlias() throws Exception {
		synapseClient.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, emailAlias);

		assertTrue(EmailValidationUtil.doesFileExist(emailAliasS3Key, 2000L));

		String emailContent = EmailValidationUtil.readFile(emailAliasS3Key);

		assertTrue(emailContent.contains("Reset Synapse Password"));
	}
	
	@Test
	public void testSendResetPasswordEmailToWrongAlias() throws Exception {
		String missingAlias = UUID.randomUUID().toString() + "@synapse.org";
		String missingAliasS3Key = EmailValidationUtil.getBucketKeyForEmail(missingAlias);

		synapseClient.sendNewPasswordResetEmail(SYNAPSE_ENDPOINT, missingAlias);

		assertFalse(EmailValidationUtil.doesFileExist(missingAliasS3Key, 2000L));
	}
	
	@Test
	public void testGetSecretKey() throws Exception {
		String apikey = synapseClient.retrieveApiKey();
		assertNotNull(apikey);
		System.out.println(apikey);
		
		// Use the API key
		synapseClient.deleteSessionTokenHeader();
		synapseClient.setUsername(username);
		synapseClient.setApiKey(apikey);
		
		// Should work
		synapseClient.getMyProfile();
	}
	
	@Test
	public void testInvalidateSecretKey() throws Exception {
		String apikey = synapseClient.retrieveApiKey();
		synapseClient.invalidateApiKey();
		String secondKey = synapseClient.retrieveApiKey();
		
		// Should be different from the first one
		assertFalse(apikey.equals(secondKey));
	}

	@Test
	public void testGetOAuth2AuthenticationUrl() throws SynapseException{
		String rediect = "https://domain.com";
		OAuthUrlRequest request = new OAuthUrlRequest();
		request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
		request.setRedirectUrl(rediect);
		OAuthUrlResponse response = synapseClient.getOAuth2AuthenticationUrl(request);
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
			synapseClient.validateOAuthAuthenticationCodeForAccessToken(request);
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
	public void testCreateAccountViaOAuth2ForAccessToken() throws SynapseException {
		try {
			OAuthAccountCreationRequest request = new OAuthAccountCreationRequest();
			request.setProvider(OAuthProvider.GOOGLE_OAUTH_2_0);
			request.setRedirectUrl("https://www.synapse.org");
			// this invalid code will trigger a SynapseForbiddenException
			request.setAuthenticationCode("test auth code");
			request.setUserName("uname");
			synapseClient.createAccountViaOAuth2ForAccessToken(request);
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
			request.setRedirectUrl("https://www.synapse.org");
			// this invalid code will trigger a SynapseForbiddenException
			request.setAuthenticationCode("test auth code");
			synapseClient.bindOAuthProvidersUserId(request);
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
	public void testUnbindExternalId() throws SynapseException {
		assertThrows(SynapseNotFoundException.class, () -> {			
			synapseClient.unbindOAuthProvidersUserId(OAuthProvider.ORCID, "http://orcid.org/1234-5678-9876-5432");
		});
	}
	
	// Test to reproduce: https://sagebionetworks.jira.com/browse/PLFM-7248
	@Test
	public void testSendPasswordResetEmailWithMissingProtocol() throws SynapseException {
		String errorMessage = assertThrows(SynapseBadRequestException.class, () -> {
			synapseClient.sendNewPasswordResetEmail("www.synapse.org", email);
		}).getMessage();
		
		assertTrue(errorMessage.contains("The provided endpoint creates an invalid URL with exception: java.net.MalformedURLException: no protocol:"));
	}

}
