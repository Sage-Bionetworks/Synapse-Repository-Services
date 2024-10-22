package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.principal.AccountCreationToken;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.NotificationEmail;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.util.SerializationUtils;

@ExtendWith(ITTestExtension.class)
public class IT502SynapseJavaClientAccountTest {
	private static SynapseAdminClient synapseAnonymous;
	private Long user2ToDelete;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	private String tosLatestVersion;
	
	public IT502SynapseJavaClientAccountTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);
	}
	
	private String s3KeyToDelete;
	
	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		s3KeyToDelete = null;
		tosLatestVersion = synapse.getTermsOfServiceInfo().getLatestTermsOfServiceVersion();
	}
	
	@AfterEach
	public void after() throws Exception {
		if (s3KeyToDelete!=null) {
			EmailValidationUtil.deleteFile(s3KeyToDelete);
			s3KeyToDelete=null;
		}
		if (user2ToDelete!=null) {
			try {
				adminSynapse.deleteUser(user2ToDelete);
			} catch (SynapseException e) { }
		}
	}
	
	private String getTokenFromFile(String key, String endpoint) throws Exception {
		return EmailValidationUtil.getTokenFromFile(key, "href=\""+endpoint, "\">");
	}
		
	@Test
	public void testCreateNewAccount() throws Exception {
		String email = UUID.randomUUID().toString()+"@foo.com";
		s3KeyToDelete = EmailValidationUtil.getBucketKeyForEmail(email);
		NewUser user = new NewUser();
		user.setEmail(email);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		String endpoint = "https://www.synapse.org?";
		synapseAnonymous.newAccountEmailValidation(user, endpoint);
		String encodedToken = getTokenFromFile(s3KeyToDelete, endpoint);
		AccountCreationToken token = SerializationUtils.hexDecodeAndDeserialize(encodedToken, AccountCreationToken.class);
		AccountSetupInfo accountSetupInfo = new AccountSetupInfo();
		accountSetupInfo.setEmailValidationSignedToken(token.getEmailValidationSignedToken());
		accountSetupInfo.setFirstName("firstName");
		accountSetupInfo.setLastName("lastName");
		accountSetupInfo.setPassword(UUID.randomUUID().toString());
		String username = UUID.randomUUID().toString();
		accountSetupInfo.setUsername(username);
		LoginResponse loginResponse = synapseAnonymous.createNewAccountForAccessToken(accountSetupInfo);
		assertNotNull(loginResponse.getAccessToken());
		// need to get the ID of the new user to delete it
		SynapseClientImpl sc = new SynapseClientImpl();
		sc.setSessionToken(loginResponse.getAccessToken());
		SynapseClientHelper.setEndpoints(sc);
		sc.setUsername(username);
		sc.signTermsOfUse(loginResponse.getAccessToken(), tosLatestVersion);
		UserProfile up = sc.getMyProfile();
		user2ToDelete = Long.parseLong(up.getOwnerId());
	}
	
	@Test
	public void testCreateNewAccountorAccessToken() throws Exception {
		String email = UUID.randomUUID().toString()+"@foo.com";
		s3KeyToDelete = EmailValidationUtil.getBucketKeyForEmail(email);
		NewUser user = new NewUser();
		user.setEmail(email);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		String endpoint = "https://www.synapse.org?";
		synapseAnonymous.newAccountEmailValidation(user, endpoint);
		String encodedToken = getTokenFromFile(s3KeyToDelete, endpoint);
		AccountCreationToken token = SerializationUtils.hexDecodeAndDeserialize(encodedToken, AccountCreationToken.class);
		AccountSetupInfo accountSetupInfo = new AccountSetupInfo();
		accountSetupInfo.setEmailValidationSignedToken(token.getEmailValidationSignedToken());
		accountSetupInfo.setFirstName("firstName");
		accountSetupInfo.setLastName("lastName");
		accountSetupInfo.setPassword(UUID.randomUUID().toString());
		String username = UUID.randomUUID().toString();
		accountSetupInfo.setUsername(username);
		LoginResponse loginResponse = synapseAnonymous.createNewAccountForAccessToken(accountSetupInfo);
		assertNotNull(loginResponse.getAccessToken());
		// need to get the ID of the new user to delete it
		SynapseClientImpl sc = new SynapseClientImpl();
		sc.setBearerAuthorizationToken(loginResponse.getAccessToken());
		SynapseClientHelper.setEndpoints(sc);
		sc.setUsername(username);
		sc.signTermsOfUse(loginResponse.getAccessToken(), tosLatestVersion);
		UserProfile up = sc.getMyProfile();
		user2ToDelete = Long.parseLong(up.getOwnerId());
	}
	
	@Test
	public void testAddEmail() throws Exception {
		// start the email validation process
		String email = UUID.randomUUID().toString()+"@foo.com";
		s3KeyToDelete = EmailValidationUtil.getBucketKeyForEmail(email);
		assertFalse(EmailValidationUtil.doesFileExist(s3KeyToDelete, 2000L));
		String endpoint = "https://www.synapse.org?";
		synapse.additionalEmailValidation(
				Long.parseLong(synapse.getMyProfile().getOwnerId()), 
				email, endpoint);
		
		// complete the email addition
		String encodedToken = getTokenFromFile(s3KeyToDelete, endpoint);
		EmailValidationSignedToken token = SerializationUtils.hexDecodeAndDeserialize(encodedToken, EmailValidationSignedToken.class);
		// we are _not_ setting it to be the notification email
		synapse.addEmail(token, false);
		// check also that 'false' can be set by omitting the parameter
		synapse.addEmail(token, null);
		
		// now remove the email
		synapse.removeEmail(email);
	}
	
	@Test
	public void testNotificationEmail() throws SynapseException {
		UserProfile up = synapse.getMyProfile();
		assertEquals(1, up.getEmails().size());
		String myEmail = up.getEmails().get(0);
		NotificationEmail notificationEmail = synapse.getNotificationEmail();
		// the current notification email is the one/only email that I have
		assertEquals(myEmail, notificationEmail.getEmail());
		// no-op, just checking that everything's wired up right
		synapse.setNotificationEmail(myEmail);
	}
	
	@Test
	public void testCheckAliasAvailable() throws SynapseException{
		AliasCheckRequest request = new AliasCheckRequest();
		// This is valid but already in use
		request.setAlias("public");
		request.setType(AliasType.TEAM_NAME);
		AliasCheckResponse response = synapse.checkAliasAvailable(request);
		assertNotNull(response);
		assertTrue(response.getValid());
		assertFalse(response.getAvailable(), "The 'public' group name should already have this alias so it cannot be available!");
	}

	@Test
	public void testGetPrincipalAlias() throws Exception {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("anonymous");
		request.setType(AliasType.USER_NAME);
		PrincipalAliasResponse response = synapse.getPrincipalAlias(request);
		assertNotNull(response);
		assertEquals(response.getPrincipalId(), BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}

}
