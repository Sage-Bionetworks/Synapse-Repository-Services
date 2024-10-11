package org.sagebionetworks.repo.model.dbo.auth;



import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TermsOfServiceAgreement;
import org.sagebionetworks.repo.model.auth.TermsOfServiceRequirements;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAuthenticatedOn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
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
	private DBOAuthenticatedOn authOn;
	
	private static final Date VALIDATED_ON = new Date();

	
	@BeforeEach
	public void setUp() throws Exception {
		authDAO.clearTermsOfServiceData();
		groupsToDelete = new ArrayList<String>();
		
		// Initialize a UserGroup
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		userId = userGroupDAO.create(ug);
	
		groupsToDelete.add(userId.toString());

		// Make a row of Credentials
		credential = new DBOCredential();
		credential.setPrincipalId(userId);
		credential.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");
		credential.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		credential.setEtag(UUID.randomUUID().toString());
		credential = basicDAO.createNew(credential);
		
		authOn = new DBOAuthenticatedOn();
		authOn.setPrincipalId(userId);
		authOn.setAuthenticatedOn(VALIDATED_ON);
		authOn.setEtag(UUID.randomUUID().toString());
		authOn = basicDAO.createNew(authOn);
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (String toDelete: groupsToDelete) {
			userGroupDAO.delete(toDelete);
		}
		authDAO.clearTermsOfServiceData();
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
	public void testChangePassword() throws Exception {
		// The original credentials should authenticate correctly
		assertTrue(authDAO.checkUserCredentials(userId, credential.getPassHash()));
		
		assertFalse(authDAO.getPasswordModifiedOn(userId).isPresent());
		assertFalse(authDAO.getPasswordExpiresOn(userId).isPresent());
		
		Instant now = Instant.now().minus(10, ChronoUnit.SECONDS);
		
		// Change the password and try to authenticate again
		authDAO.changePassword(credential.getPrincipalId(), "Bibbity Boppity BOO!");
		
		// This time it should fail
		assertFalse(authDAO.checkUserCredentials(userId, credential.getPassHash()));
		
		assertTrue(authDAO.getPasswordModifiedOn(userId).get().toInstant().isAfter(now));
		assertTrue(authDAO.getPasswordExpiresOn(userId).get().toInstant().isAfter(now.plus(DBOCredential.MAX_PASSWORD_VALIDITY_DAYS, ChronoUnit.DAYS)));

	}
	
	@Test
	public void testSecretKey() throws Exception {
		Long userId = credential.getPrincipalId();
		
		// Getter should work
		assertEquals(credential.getSecretKey(), authDAO.getSecretKey(userId));
		
		// Setter should work
		authDAO.changeSecretKey(userId);
		assertFalse(credential.getSecretKey().equals(authDAO.getSecretKey(userId)));
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
	
	@Test
	public void testGetPasswordSalt_InvalidUser() throws Exception {
		assertThrows(NotFoundException.class, ()->{
			authDAO.getPasswordSalt(-99);
		});
	}
	
	@Test
	public void testBootstrap() throws Exception {
		authDAO.bootstrap();
		// Most bootstrapped users should have signed the terms
		List<BootstrapPrincipal> ugs = userGroupDAO.getBootstrapPrincipals();
		for (BootstrapPrincipal agg: ugs) {
			if (agg instanceof BootstrapUser && !AuthorizationUtils.isUserAnonymous(agg.getId())) {
				MapSqlParameterSource param = new MapSqlParameterSource();
				param.addValue("principalId", agg.getId());
				DBOCredential creds = basicDAO.getObjectByPrimaryKey(DBOCredential.class, param).get();
			}
		}
		
		// Migration admin should have a specific API key
		String secretKey = authDAO.getSecretKey(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertEquals(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey(), secretKey);
		TermsOfServiceRequirements requirements = authDAO.getCurrentTermsOfServiceRequirements();
		assertEquals(requirements.getMinimumTermsOfServiceVersion(), authDAO.getTermsOfServiceLatestVersion());
	}
	
	@Test
	public void testGetSessionValidatedOn() {
		// if no validation date, return null
		assertNull(authDAO.getAuthenticatedOn(999999L));
		
		// check that 'userId's validation date is as expected
		Date validatedOn = authDAO.getAuthenticatedOn(userId);
		assertEquals(VALIDATED_ON.getTime(), validatedOn.getTime());
		
		
	}
	
	@Test
	public void testSetAuthenticatedOn() {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("principalId", userId);
		DBOAuthenticatedOn original = basicDAO.getObjectByPrimaryKey(DBOAuthenticatedOn.class, param).get();
		
		Date newAuthOn = new Date(original.getAuthenticatedOn().getTime()+10000L);
		
		//method under test
		authDAO.setAuthenticatedOn(userId, newAuthOn);
		
		DBOAuthenticatedOn updated = basicDAO.getObjectByPrimaryKey(DBOAuthenticatedOn.class, param).get();
		// check that date has been set
		assertEquals(newAuthOn, updated.getAuthenticatedOn());
		// check that etag has changed
		assertNotEquals(original.getEtag(), updated.getEtag());
	}
	
	@Test
	public void testSetGetTwoFactorAuthState() {
		
		assertFalse(authDAO.isTwoFactorAuthEnabled(userId));
		
		String userEtag = userGroupDAO.get(userId).getEtag();
		
		// Call under test
		authDAO.setTwoFactorAuthState(userId, true);
		
		assertTrue(authDAO.isTwoFactorAuthEnabled(userId));
		assertNotEquals(userEtag, userEtag = userGroupDAO.get(userId).getEtag());
		
		// Call under test
		authDAO.setTwoFactorAuthState(userId, false);
		
		assertFalse(authDAO.isTwoFactorAuthEnabled(userId));
		assertNotEquals(userEtag, userEtag = userGroupDAO.get(userId).getEtag());
	}
	
	@Test
	public void testGetTwoFactorAuthStateMap() {
		authDAO.setTwoFactorAuthState(userId, true);
		
		Map<Long, Boolean> expected = Map.of(
			-123L, false,
			userId, true
		);
		// Call under test
		Map<Long, Boolean> result = authDAO.getTwoFactorAuthStateMap(Set.of(-123L, userId));
		
		assertEquals(expected, result);
	}
		
	@Test
	public void testGetAndSetTermsOfServiceRequirements() {
		
		assertThrows(NotFoundException.class, () -> {			
			authDAO.getCurrentTermsOfServiceRequirements();
		});
		
		TermsOfServiceRequirements nextVersion = new TermsOfServiceRequirements()
			.setMinimumTermsOfServiceVersion("1.0.0")
			.setRequirementDate(Date.from(Instant.now()));
		
		assertEquals(nextVersion, authDAO.setCurrentTermsOfServiceRequirements(userId, nextVersion.getMinimumTermsOfServiceVersion(), nextVersion.getRequirementDate()));
		
		assertEquals(nextVersion, authDAO.getCurrentTermsOfServiceRequirements());
		
		assertEquals("A TOS requirement with the 1.0.0 minimum version already exists.", assertThrows(IllegalArgumentException.class, () -> {			
			authDAO.setCurrentTermsOfServiceRequirements(userId, nextVersion.getMinimumTermsOfServiceVersion(), nextVersion.getRequirementDate());
		}).getMessage());
		
		nextVersion.setMinimumTermsOfServiceVersion("2.0.0");
		
		assertEquals(nextVersion, authDAO.setCurrentTermsOfServiceRequirements(userId, nextVersion.getMinimumTermsOfServiceVersion(), nextVersion.getRequirementDate()));
		
		assertEquals(nextVersion, authDAO.getCurrentTermsOfServiceRequirements());
	}

	@Test
	public void testAddAndGetTermsOfServiceAgreement() {
		// Call under test
		assertEquals(Optional.empty(), authDAO.getLatestTermsOfServiceAgreement(userId));
		
		TermsOfServiceAgreement expected = new TermsOfServiceAgreement()
			.setVersion("0.0.0")
			.setAgreedOn(new Date());
		
		assertEquals(expected, authDAO.addTermsOfServiceAgreement(userId, expected.getVersion(), expected.getAgreedOn()));
		// Ignore re-sign attempts
		assertEquals(expected, authDAO.addTermsOfServiceAgreement(userId, expected.getVersion(), expected.getAgreedOn()));
		assertEquals(Optional.of(expected), authDAO.getLatestTermsOfServiceAgreement(userId));
	}
	
	@Test
	public void testGetAndSetTermsOfServiceLatestVersion() {

		assertThrows(NotFoundException.class, () -> {
			authDAO.getTermsOfServiceLatestVersion();
		});
		
		authDAO.setTermsOfServiceLatestVersion("1.0.0");
		
		assertEquals("1.0.0", authDAO.getTermsOfServiceLatestVersion());
		
		authDAO.setTermsOfServiceLatestVersion("1.0.1");
		
		assertEquals("1.0.1", authDAO.getTermsOfServiceLatestVersion());
	}

}
