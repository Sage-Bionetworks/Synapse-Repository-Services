package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class DBOAuthenticationDAOImplTest {
	
	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
		
	List<String> groupsToDelete;
	
	private static final String GROUP_NAME = "auth-test-group";
	private DBOCredential secretRow;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		
		// Initialize a UserGroup
		UserGroup ug = userGroupDAO.findGroup(GROUP_NAME, true);
		if (ug == null) {
			ug = new UserGroup();
			ug.setName(GROUP_NAME);
			ug.setIsIndividual(true);
			ug.setId(userGroupDAO.create(ug));
		}
		groupsToDelete.add(ug.getId());
		Long principalId = Long.parseLong(ug.getId());

		// Make a row of Credentials but apply it yet
		secretRow = new DBOCredential();
		secretRow.setPrincipalId(principalId);
		secretRow.setValidatedOn(new Date());
		secretRow.setSessionToken("Hsssssss...");
		secretRow.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");
		secretRow.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		secretRow.setAgreesToTermsOfUse(true);
	}

	@After
	public void tearDown() throws Exception {
		for (String toDelete: groupsToDelete) {
			userGroupDAO.delete(toDelete);
		}
	}
	
	@Test
	public void testCheckEmailAndPassword() throws Exception {
		basicDAO.update(secretRow);
		
		// Valid combination
		Long principalId = authDAO.checkEmailAndPassword(GROUP_NAME, secretRow.getPassHash());
		assertEquals(secretRow.getPrincipalId(), principalId);
		
		// Invalid combinations
		try {
			authDAO.checkEmailAndPassword(GROUP_NAME, "Blargle");
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
		
		try {
			authDAO.checkEmailAndPassword("Blargle", secretRow.getPassHash());
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
		
		try {
			authDAO.checkEmailAndPassword("Blargle", "Blargle");
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testSessionTokenCRUD() throws Exception {
		basicDAO.update(secretRow);
		
		// Get by username
		Session session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
		
		// Get by token
		Long id = authDAO.getPrincipalIfValid(secretRow.getSessionToken());
		assertEquals(secretRow.getPrincipalId(), id);
		
		// Delete
		authDAO.deleteSessionToken(secretRow.getSessionToken());
		session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertNull(session.getSessionToken());
		
		// Change to a string
		String foobarSessionToken = "foobar";
		authDAO.changeSessionToken(secretRow.getPrincipalId().toString(), foobarSessionToken);
		session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertEquals(foobarSessionToken, session.getSessionToken());
		
		// Change to a UUID
		authDAO.changeSessionToken(secretRow.getPrincipalId().toString(), null);
		session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertFalse(foobarSessionToken.equals(session.getSessionToken()));
		assertFalse(secretRow.getSessionToken().equals(session.getSessionToken()));
	}
	
	@Test
	public void testGetWithoutToUAcceptance() throws Exception {
		secretRow.setAgreesToTermsOfUse(false);
		basicDAO.update(secretRow);
		
		Session session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertNull(session);
		
		Long id = authDAO.getPrincipalIfValid(secretRow.getSessionToken());
		assertNull(id);
	}
	
	@Test
	public void testSessionTokenRevalidation() throws Exception {
		// Test fast!  Only one second before expiration!
		Date almostExpired = secretRow.getValidatedOn();
		almostExpired.setTime(almostExpired.getTime() - DBOAuthenticationDAOImpl.SESSION_EXPIRATION_TIME + 1000);
		basicDAO.update(secretRow);

		// A second hasn't passed yet
		Session session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertNotNull(session);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
		
		Thread.sleep(1500);
		
		// Session should no longer be valid
		session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertNull(session);

		// Session is valid again
		authDAO.revalidateSessionToken(secretRow.getPrincipalId().toString());
		session = authDAO.getSessionTokenIfValid(GROUP_NAME);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testChangePassword() throws Exception {
		// The original credentials should authenticate correctly
		basicDAO.update(secretRow);
		Long principalId = authDAO.checkEmailAndPassword(GROUP_NAME, secretRow.getPassHash());
		assertEquals(secretRow.getPrincipalId(), principalId);
		
		// Change the password and try to authenticate again
		authDAO.changePassword(secretRow.getPrincipalId().toString(), "Bibbity Boppity BOO!");
		
		// This time it should fail
		authDAO.checkEmailAndPassword(GROUP_NAME, secretRow.getPassHash());
	}
	
	@Test
	public void testSecretKey() throws Exception {
		String userId = secretRow.getPrincipalId().toString();
		basicDAO.update(secretRow);
		
		// Getter should work
		assertEquals(secretRow.getSecretKey(), authDAO.getSecretKey(userId));
		
		// Setter should work
		authDAO.changeSecretKey(userId);
		assertFalse(secretRow.getSecretKey().equals(authDAO.getSecretKey(userId)));
	}
	
	@Test
	public void testGetPasswordSalt() throws Exception {
		basicDAO.update(secretRow);
		String passHash = PBKDF2Utils.hashPassword("password", null);
		byte[] salt = PBKDF2Utils.extractSalt(passHash);
		
		// Change the password to a valid one
		authDAO.changePassword(secretRow.getPrincipalId().toString(), passHash);
		
		// Compare the salts
		byte[] passedSalt = authDAO.getPasswordSalt(GROUP_NAME);
		assertArrayEquals(salt, passedSalt);
	}
	
	@Test
	public void testSetToU() throws Exception {
		basicDAO.update(secretRow);
		String userId = secretRow.getPrincipalId().toString();
		
		// Reject the terms
		authDAO.setTermsOfUseAcceptance(userId, false);
		assertFalse(authDAO.hasUserAcceptedToU(userId));
		assertNull(authDAO.getSessionTokenIfValid(GROUP_NAME));
		
		// Accept the terms
		authDAO.setTermsOfUseAcceptance(userId, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId));
		
		// Pretend we haven't had a chance to see the terms yet
		authDAO.setTermsOfUseAcceptance(userId, null);
		assertFalse(authDAO.hasUserAcceptedToU(userId));
		
		// Accept the terms again
		authDAO.setTermsOfUseAcceptance(userId, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId));
	}
	
	@Test
	public void testBootstrapCredentials() throws Exception {
		if (!StackConfiguration.isProductionStack()) {
			String testUsers[] = new String[] { 
					StackConfiguration.getIntegrationTestUserAdminName(), 
					StackConfiguration.getIntegrationTestRejectTermsOfUseEmail(), 
					StackConfiguration.getIntegrationTestUserOneEmail(), 
					StackConfiguration.getIntegrationTestUserTwoName(), 
					StackConfiguration.getIntegrationTestUserThreeEmail() };
			String testPasswords[] = new String[] { 
					StackConfiguration.getIntegrationTestUserAdminPassword(), 
					StackConfiguration.getIntegrationTestRejectTermsOfUsePassword(), 
					StackConfiguration.getIntegrationTestUserOnePassword(), 
					StackConfiguration.getIntegrationTestUserTwoPassword(), 
					StackConfiguration.getIntegrationTestUserThreePassword() };
			for (int i = 0; i < testUsers.length; i++) {
				String passHash = PBKDF2Utils.hashPassword(testPasswords[i], authDAO.getPasswordSalt(testUsers[i]));
				authDAO.checkEmailAndPassword(testUsers[i], passHash);
			}
		}
		
		// Most bootstrapped users should have signed the terms
		List<UserGroupInt> ugs = userGroupDAO.getBootstrapUsers();
		for (UserGroupInt ug : ugs) {
			if (ug.getIsIndividual() 
					&& !ug.getName().equals(StackConfiguration.getIntegrationTestRejectTermsOfUseEmail())
					&& !AuthorizationUtils.isUserAnonymous(ug.getName())) {
				MapSqlParameterSource param = new MapSqlParameterSource();
				param.addValue("principalId", ug.getId());
				DBOCredential creds = basicDAO.getObjectByPrimaryKey(DBOCredential.class, param);
				assertTrue(creds.getAgreesToTermsOfUse());
			}
		}
	}
}
