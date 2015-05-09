package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;

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
	
	private File fileToDelete;
	
	@Before
	public void before() throws SynapseException {
		fileToDelete = null;
	}
	
	@After
	public void after() throws Exception {
		if (fileToDelete!=null) {
			fileToDelete.delete();
			fileToDelete=null;
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
	
	private String getTokenFromFile(File file, String endpoint) throws IOException {
		return EmailValidationUtil.getTokenFromFile(file, "href=\\\""+endpoint, "\\\">");
	}
		
	@Test
	public void testCreateNewAccount() throws Exception {
		String email = UUID.randomUUID().toString()+"@foo.com";
		fileToDelete = EmailValidationUtil.getFileForEmail(email);
		assertNotNull(fileToDelete.toString(), fileToDelete);
		NewUser user = new NewUser();
		user.setEmail(email);
		user.setFirstName("firstName");
		user.setLastName("lastName");
		String endpoint = "https://www.synapse.org?";
		synapseAnonymous.newAccountEmailValidation(user, endpoint);
		String token = getTokenFromFile(fileToDelete, endpoint);
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
		fileToDelete = EmailValidationUtil.getFileForEmail(email);
		assertNotNull(fileToDelete);
		assertTrue(fileToDelete.exists());
		String endpoint = "https://www.synapse.org?";
		synapseOne.additionalEmailValidation(
				Long.parseLong(synapseOne.getMyProfile().getOwnerId()), 
				email, endpoint);
		
		// complete the email addition
		String token = getTokenFromFile(fileToDelete, endpoint);
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
	

}
