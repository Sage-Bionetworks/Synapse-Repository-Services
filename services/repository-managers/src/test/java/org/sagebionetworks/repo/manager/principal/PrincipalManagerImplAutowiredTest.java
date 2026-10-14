package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.admin.UpdateNotificationEmailRequest;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.NotificationEmail;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PrincipalManagerImplAutowiredTest {

	@Autowired
	private PrincipalManager principalManager;

	@Autowired
	private PrincipalAliasDAO principalAliasDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserProfileDAO userProfileDAO;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private NotificationEmailDAO notificationEmailDao;

	@Autowired
	private UserProfileManager userProfileManager;

	private UserInfo adminUserInfo;
	private UserInfo testUser;

	private final String password = "A User's Pa$$word456";

	private String fileHandleId;

	private String originalEmail;

	@BeforeEach
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		NewUser nu = new NewUser();
		originalEmail = UUID.randomUUID().toString() + "@test.com";
		nu.setEmail(originalEmail);
		nu.setUserName(UUID.randomUUID().toString());
		testUser = userManager.createOrGetTestUser(adminUserInfo, nu);
		authenticationManager.setPassword(testUser.getId(), password);
		S3FileHandle fh = new S3FileHandle();
		fh.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh.setKey("key.jpg");
		fh.setBucketName("bucket");
		fh.setContentType("image/jpg");
		fh.setEtag("etag");
		fh.setCreatedBy(testUser.getId().toString());
		fh.setFileName("profile pic.jpg");
		fileHandleId = fileHandleDao.createFile(fh).getId();
	}
	
	@AfterEach
	public void after() throws Exception {
		principalAliasDao.removeAllAliasFromPrincipal(testUser.getId());
		userProfileDAO.delete(testUser.getId().toString());
		fileHandleDao.delete(fileHandleId);
	}


	@Test
	public void testClearPrincipal() {
		String username = UUID.randomUUID().toString();

		// Add some aliases to make sure they get removed
		PrincipalAlias orcid = new PrincipalAlias();
		orcid.setPrincipalId(testUser.getId());
		orcid.setType(AliasType.USER_ORCID);
		orcid.setAlias("https://orcid.org/0000-0000-0000-0000");
		principalAliasDao.bindAliasToPrincipal(orcid);

		PrincipalAlias usernameAlias = new PrincipalAlias();
		usernameAlias.setPrincipalId(testUser.getId());
		usernameAlias.setType(AliasType.USER_NAME);
		usernameAlias.setAlias(username);
		principalAliasDao.bindAliasToPrincipal(usernameAlias);
		
		String orcIdSubject = "0000-0000-0000-0000";
		
		userManager.bindUserToOidcSubject(orcid, OAuthProvider.ORCID, orcIdSubject);
		
		assertTrue(userManager.lookupOidcBindingBySubject(OAuthProvider.ORCID, orcIdSubject).isPresent());

		// Verify that we can log in before we change the password
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername(username);
		loginRequest.setPassword(password);
		authenticationManager.login(loginRequest, null);

		// Modify the existing profile to ensure content changes
		UserProfile profile = userProfileDAO.get(testUser.getId().toString());
		profile.setUserName(username);
		profile.setDisplayName("Some display name");
		profile.setFirstName("First");
		profile.setLastName("Last");
		profile.setPosition("Job");
		profile.setCompany("Organization");
		profile.setEmails(Collections.singletonList("email1@gmail.com"));
		profile.setLocation("Seattle");
		profile.setOpenIds(Collections.singletonList("OpenID1"));
		profile.setProfilePicureFileHandleId(fileHandleId);
		profile.setRStudioUrl("https://foo.bar");
		profile.setSummary("this is my bio");
		profile.setTeamName("awesome team");
		profile.setUrl("https://all.about.me");
		userProfileDAO.update(profile);

		UserInfo adminUserInfo = new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);

		// Call under test
		principalManager.clearPrincipalInformation(adminUserInfo, testUser.getId());

		String expectedEmail = "gdpr-synapse+" + testUser.getId() + "@sagebase.org";
		// Verify that information has been cleared.
		profile = userProfileDAO.get(testUser.getId().toString());
		assertEquals(expectedEmail, profile.getEmail());
		assertEquals(Collections.singletonList(expectedEmail), profile.getEmails());
		assertEquals("", profile.getFirstName());
		assertEquals("", profile.getLastName());
		assertEquals(Collections.emptyList(), profile.getOpenIds());
		assertFalse(profile.getNotificationSettings().getSendEmailNotifications());
		assertNull(profile.getDisplayName());
		assertNull(profile.getIndustry());
		assertNull(profile.getProfilePicureFileHandleId());
		assertNull(profile.getLocation());
		assertNull(profile.getCompany());
		assertNull(profile.getPosition());
		List<PrincipalAlias> pas = principalAliasDao.listPrincipalAliases(testUser.getId());
		assertEquals(1, pas.size());
		assertEquals(AliasType.USER_EMAIL, pas.get(0).getType());
		assertEquals("gdpr-synapse+" + testUser.getId() + "@sagebase.org", pas.get(0).getAlias());
		
		assertNull(profile.getRStudioUrl());
		assertNull(profile.getSummary());
		assertNull(profile.getTeamName());
		assertNull(profile.getUrl());

		assertFalse(userManager.lookupOidcBindingBySubject(OAuthProvider.ORCID, orcIdSubject).isPresent());
		
		// Verify that the password has been changed
		assertThrows(UnauthenticatedException.class, () -> authenticationManager.login(loginRequest, null));
	}

	@Test
	public void testUpdateNotificationEmailForUserWithNewAddress() {
		String newEmail = UUID.randomUUID().toString() + "@test.com";
		UpdateNotificationEmailRequest request = new UpdateNotificationEmailRequest().setEmail(newEmail);

		assertEquals(originalEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));

		// Call under test
		NotificationEmail result = principalManager.updateNotificationEmailForUser(adminUserInfo, testUser.getId(), request);

		assertEquals(new NotificationEmail().setEmail(newEmail), result);
		assertEquals(newEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));

		// The new address is bound in addition to the old one, so the profile lists both
		UserProfile profile = userProfileManager.getUserProfile(testUser.getId().toString());
		assertTrue(profile.getEmails().contains(originalEmail));
		assertTrue(profile.getEmails().contains(newEmail));
	}

	@Test
	public void testUpdateNotificationEmailForUserWithRemovePrevious() {
		String newEmail = UUID.randomUUID().toString() + "@test.com";
		UpdateNotificationEmailRequest request = new UpdateNotificationEmailRequest().setEmail(newEmail)
				.setRemovePreviousNotificationEmail(true);

		// Call under test
		NotificationEmail result = principalManager.updateNotificationEmailForUser(adminUserInfo, testUser.getId(), request);

		assertEquals(new NotificationEmail().setEmail(newEmail), result);
		assertEquals(newEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));

		UserProfile profile = userProfileManager.getUserProfile(testUser.getId().toString());
		assertFalse(profile.getEmails().contains(originalEmail));
		assertTrue(profile.getEmails().contains(newEmail));
		assertTrue(principalAliasDao.isAliasAvailable(originalEmail));
	}

	@Test
	public void testUpdateNotificationEmailForUserWithNoNotificationEmailRow() {
		// NOTIFICATION_EMAIL.ALIAS_ID cascades on delete, so dropping every alias also drops the
		// notification email row. This is the only branch that a mock-based test cannot prove, since
		// NotificationEmailDAO.update is a silent no-op when the principal has no row.
		principalAliasDao.removeAllAliasFromPrincipal(testUser.getId());
		assertThrows(NotFoundException.class, () -> notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));

		String newEmail = UUID.randomUUID().toString() + "@test.com";
		UpdateNotificationEmailRequest request = new UpdateNotificationEmailRequest().setEmail(newEmail);

		// Call under test
		NotificationEmail result = principalManager.updateNotificationEmailForUser(adminUserInfo, testUser.getId(), request);

		assertEquals(new NotificationEmail().setEmail(newEmail), result);
		assertEquals(newEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));
	}

	@Test
	public void testUpdateNotificationEmailForUserWithSameAddressTwice() {
		String newEmail = UUID.randomUUID().toString() + "@test.com";
		UpdateNotificationEmailRequest request = new UpdateNotificationEmailRequest().setEmail(newEmail)
				.setRemovePreviousNotificationEmail(true);

		NotificationEmail first = principalManager.updateNotificationEmailForUser(adminUserInfo, testUser.getId(), request);

		// Call under test - the repeat call is a no-op rather than unbinding the address it just set
		NotificationEmail second = principalManager.updateNotificationEmailForUser(adminUserInfo, testUser.getId(), request);

		assertEquals(first, second);
		assertEquals(newEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));
		assertFalse(principalAliasDao.isAliasAvailable(newEmail));
	}

	@Test
	public void testUpdateNotificationEmailForUserWithNonAdmin() {
		UpdateNotificationEmailRequest request = new UpdateNotificationEmailRequest()
				.setEmail(UUID.randomUUID().toString() + "@test.com");

		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			principalManager.updateNotificationEmailForUser(testUser, testUser.getId(), request);
		});

		assertEquals(originalEmail, notificationEmailDao.getNotificationEmailForPrincipal(testUser.getId()));
	}
}
