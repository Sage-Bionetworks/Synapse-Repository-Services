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
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
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
	private DBOCredential secretRow;
	private static String userEtag;

	
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
		secretRow = new DBOCredential();
		secretRow.setPrincipalId(userId);
		secretRow.setValidatedOn(new Date());
		secretRow.setSessionToken("Hsssssss...");
		secretRow.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");
		secretRow.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		secretRow.setAgreesToTermsOfUse(true);
		secretRow = basicDAO.createNew(secretRow);
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
		Long principalId = authDAO.checkUserCredentials(userId, secretRow.getPassHash());
		assertEquals(secretRow.getPrincipalId(), principalId);
		
		// Invalid combinations
		try {
			authDAO.checkUserCredentials(userId, "Blargle");
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
		
		try {
			authDAO.checkUserCredentials(-99, secretRow.getPassHash());
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
		
		try {
			authDAO.checkUserCredentials(-100, "Blargle");
			fail("That combination should not have succeeded");
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testSessionTokenCRUD() throws Exception {
		// Get by username
		Session session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
		assertEquals(secretRow.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());

		// Get by token
		Long id = authDAO.getPrincipalIfValid(secretRow.getSessionToken());
		assertEquals(secretRow.getPrincipalId(), id);
		
		// Get by token, without restrictions
		Long principalId = authDAO.getPrincipal(secretRow.getSessionToken());
		assertEquals(secretRow.getPrincipalId(), principalId);
		
		// Delete
		authDAO.deleteSessionToken(secretRow.getSessionToken());
		session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertNull(session.getSessionToken());
		assertEquals(secretRow.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
		
		// Change to a string
		String foobarSessionToken = "foobar";
		authDAO.changeSessionToken(secretRow.getPrincipalId(), foobarSessionToken, DomainType.SYNAPSE);
		session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertEquals(foobarSessionToken, session.getSessionToken());
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
		
		// Change to a UUID
		authDAO.changeSessionToken(secretRow.getPrincipalId(), null, DomainType.SYNAPSE);
		session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertFalse(foobarSessionToken.equals(session.getSessionToken()));
		assertFalse(secretRow.getSessionToken().equals(session.getSessionToken()));
		assertEquals(secretRow.getAgreesToTermsOfUse(), session.getAcceptsTermsOfUse());
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate(principalId.toString());
		assertTrue(!userEtag.equals(changedEtag));
	}
	
	@Test
	public void testGetWithoutToUAcceptance() throws Exception {
		secretRow.setAgreesToTermsOfUse(false);
		basicDAO.update(secretRow);
		
		// Could do two things here: don't create a TOU record, or create it with
		// the flag set to false. Do the latter because it's closest to what's here.
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setPrincipalId(secretRow.getPrincipalId());
		tou.setDomain(DomainType.SYNAPSE);
		tou.setAgreesToTermsOfUse(Boolean.FALSE);
		basicDAO.createOrUpdate(tou);
		
		Session session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertNotNull(session);
		
		Long id = authDAO.getPrincipalIfValid(secretRow.getSessionToken());
		assertNotNull(id);
	}
	
	@Test
	public void testSessionTokenRevalidation() throws Exception {
		// Test fast!  Only one second before expiration!
		Date now = secretRow.getValidatedOn();
		secretRow.setValidatedOn(new Date(now.getTime() - DBOAuthenticationDAOImpl.SESSION_EXPIRATION_TIME + 1000));
		basicDAO.update(secretRow);

		// Still valid
		Session session = authDAO.getSessionTokenIfValid(userId, now, DomainType.SYNAPSE);
		assertNotNull(session);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
		
		// Right on the dot!  Too bad, that's invalid :P
		now.setTime(now.getTime() + 1000);
		session = authDAO.getSessionTokenIfValid(userId, now, DomainType.SYNAPSE);
		assertNull(session);
		
		// Session should no longer be valid
		now.setTime(now.getTime() + 1000);
		session = authDAO.getSessionTokenIfValid(userId, now, DomainType.SYNAPSE);
		assertNull(session);

		// Session is valid again
		authDAO.revalidateSessionToken(secretRow.getPrincipalId(), DomainType.SYNAPSE);
		session = authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testChangePassword() throws Exception {
		// The original credentials should authenticate correctly
		Long principalId = authDAO.checkUserCredentials(userId, secretRow.getPassHash());
		assertEquals(secretRow.getPrincipalId(), principalId);
		
		// Change the password and try to authenticate again
		authDAO.changePassword(secretRow.getPrincipalId(), "Bibbity Boppity BOO!");
		
		// This time it should fail
		authDAO.checkUserCredentials(userId, secretRow.getPassHash());
	}
	
	@Test
	public void testSecretKey() throws Exception {
		Long userId = secretRow.getPrincipalId();
		
		// Getter should work
		assertEquals(secretRow.getSecretKey(), authDAO.getSecretKey(userId));
		
		// Setter should work
		authDAO.changeSecretKey(userId);
		assertFalse(secretRow.getSecretKey().equals(authDAO.getSecretKey(userId)));
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate(userId.toString());
		assertTrue(!userEtag.equals(changedEtag));
	}
	
	@Test
	public void testGetPasswordSalt() throws Exception {
		String passHash = PBKDF2Utils.hashPassword("password", null);
		byte[] salt = PBKDF2Utils.extractSalt(passHash);
		
		// Change the password to a valid one
		authDAO.changePassword(secretRow.getPrincipalId(), passHash);
		
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
		Long userId = secretRow.getPrincipalId();
		
		// Reject the terms
		authDAO.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, false);
		assertFalse(authDAO.hasUserAcceptedToU(userId, DomainType.SYNAPSE));
		assertNotNull(authDAO.getSessionTokenIfValid(userId, DomainType.SYNAPSE));
		
		// Verify that the parent group's etag has changed
		String changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Accept the terms
		authDAO.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId, DomainType.SYNAPSE));
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Pretend we haven't had a chance to see the terms yet
		authDAO.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, null);
		assertFalse(authDAO.hasUserAcceptedToU(userId, DomainType.SYNAPSE));
		
		// Verify that the parent group's etag has changed
		userEtag = changedEtag;
		changedEtag = userGroupDAO.getEtagForUpdate("" + userId);
		assertTrue(!userEtag.equals(changedEtag));
		
		// Accept the terms again
		authDAO.setTermsOfUseAcceptance(userId, DomainType.SYNAPSE, true);
		assertTrue(authDAO.hasUserAcceptedToU(userId, DomainType.SYNAPSE));
		
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
				assertTrue(creds.getAgreesToTermsOfUse());
			}
		}
		
		// Migration admin should have a specific API key
		String secretKey = authDAO.getSecretKey(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertEquals(StackConfiguration.getMigrationAdminAPIKey(), secretKey);
	}
}
