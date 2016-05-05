package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;

public class IT502SynapseJavaClientAccountTest {
	private static SynapseAdminClient adminSynapse;
	private static SynapseAdminClient synapseAnonymous;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;
	private Long user2ToDelete;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);
	}
	
	private String s3KeyToDelete;
	
	@Before
	public void before() throws SynapseException {
		s3KeyToDelete = null;
	}
	
	@After
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
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
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
		String token = getTokenFromFile(s3KeyToDelete, endpoint);
		AccountSetupInfo accountSetupInfo = new AccountSetupInfo();
		accountSetupInfo.setEmailValidationToken(token);
		accountSetupInfo.setFirstName("firstName");
		accountSetupInfo.setLastName("lastName");
		accountSetupInfo.setPassword(UUID.randomUUID().toString());
		String username = UUID.randomUUID().toString();
		accountSetupInfo.setUsername(username);
		Session session = synapseAnonymous.createNewAccount(accountSetupInfo);
		assertNotNull(session.getSessionToken());
		// need to get the ID of the new user to delete it
		SynapseClientImpl sc = new SynapseClientImpl();
		sc.setSessionToken(session.getSessionToken());
		SynapseClientHelper.setEndpoints(sc);
		sc.setUserName(username);
		sc.signTermsOfUse(session.getSessionToken(), true);
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
		synapseOne.additionalEmailValidation(
				Long.parseLong(synapseOne.getMyProfile().getOwnerId()), 
				email, endpoint);
		
		// complete the email addition
		String token = getTokenFromFile(s3KeyToDelete, endpoint);
		AddEmailInfo addEmailInfo = new AddEmailInfo();
		addEmailInfo.setEmailValidationToken(token);
		// we are _not_ setting it to be the notification email
		synapseOne.addEmail(addEmailInfo, false);
		
		// now remove the email
		synapseOne.removeEmail(email);
	}
	
	@Test
	public void testNotificationEmail() throws SynapseException {
		UserProfile up = synapseOne.getMyProfile();
		assertEquals(1, up.getEmails().size());
		String myEmail = up.getEmails().get(0);
		String notificationEmail = synapseOne.getNotificationEmail();
		// the current notification email is the one/only email that I have
		assertEquals(myEmail, notificationEmail);
		// no-op, just checking that everything's wired up right
		synapseOne.setNotificationEmail(myEmail);
	}
	
	@Test
	public void testCheckAliasAvailable() throws SynapseException{
		AliasCheckRequest request = new AliasCheckRequest();
		// This is valid but already in use
		request.setAlias("public");
		request.setType(AliasType.TEAM_NAME);
		AliasCheckResponse response = synapseOne.checkAliasAvailable(request);
		assertNotNull(response);
		assertTrue(response.getValid());
		assertFalse("The 'public' group name should already have this alias so it cannot be available!",response.getAvailable());
	}

	@Test
	public void testGetPrincipalAlias() throws Exception {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("anonymous");
		request.setType(AliasType.USER_NAME);
		PrincipalAliasResponse response = synapseOne.getPrincipalAlias(request);
		assertNotNull(response);
		assertEquals(response.getPrincipalId(), BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}

}
