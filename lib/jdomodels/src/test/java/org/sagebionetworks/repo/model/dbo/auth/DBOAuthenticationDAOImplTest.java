package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthenticatedOn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.web.NotFoundException;
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
		
	private List<String> groupsToDelete;
	
	private Long userId;
	private DBOCredential credential;
	private DBOSessionToken sessionToken;
	private DBOAuthenticatedOn authOn;
	private DBOTermsOfUseAgreement touAgreement;
	private static String userEtag;
	
	private static final Date VALIDATED_ON = new Date();

	
	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		
		// Initialize a UserGroup
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		userId = userGroupDAO.create(ug);
	
		groupsToDelete.add(userId.toString());
		userEtag = userGroupDAO.getEtagForUpdate(userId.toString());

		// Make a row of Credentials
		credential = new DBOCredential();
		credential.setPrincipalId(userId);
		credential.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");
		credential.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		credential = basicDAO.createNew(credential);
		
		sessionToken = new DBOSessionToken();
		sessionToken.setPrincipalId(userId);
		sessionToken.setSessionToken("Hsssssss...");
		sessionToken = basicDAO.createNew(sessionToken);
		
		authOn = new DBOAuthenticatedOn();
		authOn.setPrincipalId(userId);
		authOn.setAuthenticatedOn(VALIDATED_ON);
		authOn.setEtag(UUID.randomUUID().toString());
		authOn = basicDAO.createNew(authOn);
		
		touAgreement = new DBOTermsOfUseAgreement();
		touAgreement.setPrincipalId(userId);
		touAgreement.setAgreesToTermsOfUse(true);
		touAgreement = basicDAO.createNew(touAgreement);
	}

	@After
	public void tearDown() throws Exception {
		for (String toDelete: groupsToDelete) {
			userGroupDAO.delete(toDelete);
		}
	}
	
	@Test
	public void testCheckUserCredentials() throws Exception {
		// Valid combination
		assertTrue(authDAO.checkUserCredentials(userId, credential.getPassHash()));
		
		// Invalid combinations
		assertFalse(authDAO.checkUserCredentials(userId, "Blargle"));

		assertFalse(authDAO.checkUserCredentials(-99, credential.getPassHash()));

		assertFalse(authDAO.checkUserCredentials(-100, "Blargle"));
	}
	
	@Test
	public void testCheckSessionNotSharedBetweenDomains() {
		Session session = authDAO.getSessionTokenIfValid(userId);
		assertNotNull(session);
	}

	@Test
	public void testSigningOffOnlySignsOffOneDomain() {
		// Log in to none, log out
		assertTrue(authDAO.checkUserCredentials(userId, credential.getPassHash()));
		Session session = authDAO.getSessionTokenIfValid(userId);
		authDAO.deleteSessionToken(sessionToken.getSessionToken());

		// You are still logged in to synapse
		session = authDAO.getSessionTokenIfValid(userId);
		assertNotNull(session);
	}

	@Test
	public void testSessionTokenCRUD() throws Exception {
		// Get by username
		Session session = authDAO.getSessionTokenIfValid(userId);
		assertEquals(sessionToken.getSessionToken(), session.getSessionToken());
		assertEquals(touAgreement.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());

		// Get by token
		Long id = authDAO.getPrincipalIfValid(sessionToken.getSessionToken());
		assertEquals(credential.getPrincipalId(), id);
		
		// Get by token, without restrictions
		Long principalId = authDAO.getPrincipal(sessionToken.getSessionToken());
		assertEquals(credential.getPrincipalId(), principalId);
		
		// Delete
		authDAO.deleteSessionToken(sessionToken.getSessionToken());
		session = authDAO.getSessionTokenIfValid(userId);
		assertNull(session.getSessionToken());
		assertEquals(touAgreement.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
		
		// Change to a string
		String foobarSessionToken = "foobar";
		authDAO.changeSessionToken(credential.getPrincipalId(), foobarSessionToken);
		session = authDAO.getSessionTokenIfValid(userId);
		assertEquals(foobarSessionToken, session.getSessionToken());
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
		
		// Change to a UUID
		authDAO.changeSessionToken(credential.getPrincipalId(), null);
		session = authDAO.getSessionTokenIfValid(userId);
		assertFalse(foobarSessionToken.equals(session.getSessionToken()));
		assertFalse(sessionToken.getSessionToken().equals(session.getSessionToken()));
		assertEquals(touAgreement.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
	}
	
	@Test
	public void testGetWithoutToUAcceptance() throws Exception {
		touAgreement.setAgreesToTermsOfUse(false);
		basicDAO.update(touAgreement);
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setPrincipalId(credential.getPrincipalId());
		tou.setAgreesToTermsOfUse(Boolean.FALSE);
		basicDAO.createOrUpdate(tou);
		
		Session session = authDAO.getSessionTokenIfValid(userId);
		assertNotNull(session);
		
		Long id = authDAO.getPrincipalIfValid(sessionToken.getSessionToken());
		assertNotNull(id);
	}
	
	@Test
	public void testSessionTokenRevalidation() throws Exception {
		// Test fast!  Only one second before expiration!
		Date now = authOn.getAuthenticatedOn();
		authOn.setAuthenticatedOn(new Date(now.getTime() - DBOAuthenticationDAOImpl.SESSION_EXPIRATION_TIME + 1000));
		basicDAO.update(authOn);

		// Still valid
		Session session = authDAO.getSessionTokenIfValid(userId, now);
		assertNotNull(session);
		assertEquals(sessionToken.getSessionToken(), session.getSessionToken());

		// Right on the dot!  Too bad, that's invalid :P
		now.setTime(now.getTime() + 1000);
		session = authDAO.getSessionTokenIfValid(userId, now);
		assertNull(session);
		
		// Session should no longer be valid
		now.setTime(now.getTime() + 1000);
		session = authDAO.getSessionTokenIfValid(userId, now);
		assertNull(session);

		// Session is valid again
		assertTrue(authDAO.revalidateSessionTokenIfNeeded(credential.getPrincipalId()));
		session = authDAO.getSessionTokenIfValid(userId);
		assertEquals(sessionToken.getSessionToken(), session.getSessionToken());
	}
	
	@Test
	public void testChangePassword() throws Exception {
		// The original credentials should authenticate correctly
		assertTrue(authDAO.checkUserCredentials(userId, credential.getPassHash()));
		
		// Change the password and try to authenticate again
		authDAO.changePassword(credential.getPrincipalId(), "Bibbity Boppity BOO!");
		
		// This time it should fail
		assertFalse(authDAO.checkUserCredentials(userId, credential.getPassHash()));
	}
	
	@Test
	public void testSecretKey() throws Exception {
		Long userId = credential.getPrincipalId();
		
		// Getter should work
		assertEquals(credential.getSecretKey(), authDAO.getSecretKey(userId));
		
		// Setter should work
		authDAO.changeSecretKey(userId);
		assertFalse(credential.getSecretKey().equals(authDAO.getSecretKey(userId)));
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate(userId.toString());
		assertTrue(!userEtag.equals(changedEtag));
	}
	
	@Test
	public void testGetPasswordSalt() throws Exception {
		String passHash = PBKDF2Utils.hashPassword("password", null);
		byte[] salt = PBKDF2Utils.extractSalt(passHash);
		
		// Change the password to a valid one
		authDAO.changePassword(credential.getPrincipalId(), passHash);
		
		// Compare the salts
		byte[] passedSalt = authDAO.getPasswordSalt(userId);
		assertArrayEquals(salt, passedSalt);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetPasswordSalt_InvalidUser() throws Exception {
		authDAO.getPasswordSalt(-99);
	}
	
	@Test
	public void testSetToU() throws Exception {
		Long userId = credential.getPrincipalId();
		
		// Reject the terms
		authDAO.setTermsOfUseAcceptance(userId, false);
		assertFalse(authDAO.hasUserAcceptedToU(userId));
		assertNotNull(authDAO.getSessionTokenIfValid(userId));
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Accept the terms
		authDAO.setTermsOfUseAcceptance(userId, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId));
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Pretend we haven't had a chance to see the terms yet
		authDAO.setTermsOfUseAcceptance(userId, null);
		assertFalse(authDAO.hasUserAcceptedToU(userId));
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Accept the terms again
		authDAO.setTermsOfUseAcceptance(userId, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId));
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
	}
	
	@Test
	public void testBootstrapCredentials() throws Exception {
		// Most bootstrapped users should have signed the terms
		List<BootstrapPrincipal> ugs = userGroupDAO.getBootstrapPrincipals();
		for (BootstrapPrincipal agg: ugs) {
			if (agg instanceof BootstrapUser 
					&& !AuthorizationUtils.isUserAnonymous(agg.getId())) {
				MapSqlParameterSource param = new MapSqlParameterSource();
				param.addValue("principalId", agg.getId());
				DBOCredential creds = basicDAO.getObjectByPrimaryKey(DBOCredential.class, param);
				assertTrue(touAgreement.getAgreesToTermsOfUse());
			}
		}
		
		// Migration admin should have a specific API key
		String secretKey = authDAO.getSecretKey(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertEquals(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey(), secretKey);
	}

	@Test
	public void testDeleteSessionToken_byPrincipalId(){
		String sessionToken = authDAO.changeSessionToken(userId, null);
		assertNotNull(authDAO.getSessionTokenIfValid(userId).getSessionToken());

		//method under test
		authDAO.deleteSessionToken(userId);
		assertNull(authDAO.getSessionTokenIfValid(userId).getSessionToken());
	}
	
	@Test
	public void testGetSessionValidatedOn() {
		// if no validation date, return null
		assertNull(authDAO.getAuthenticatedOn(999999L));
		
		// check that 'userId's validation date is as expected
		Date validatedOn = authDAO.getAuthenticatedOn(userId);
		assertEquals(VALIDATED_ON.getTime(), validatedOn.getTime());
		
		
	}
}
