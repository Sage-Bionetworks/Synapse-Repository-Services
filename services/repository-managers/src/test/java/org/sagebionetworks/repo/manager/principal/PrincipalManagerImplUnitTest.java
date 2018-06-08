package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.SerializationUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@RunWith(MockitoJUnitRunner.class)
public class PrincipalManagerImplUnitTest {

	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private SynapseEmailService mockSynapseEmailService;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AuthenticationManager mockAuthManager;
	@Mock
	private UserProfileDAO mockUserProfileDAO;
	@Mock
	private NotificationEmailDAO mockNotificationEmailDao;
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	private PrincipalManagerImpl manager;

	private NewUser user;
	private Date now;

	private static final Long USER_ID = 111L;
	private static final String EMAIL = "foo@bar.com";
	private static final String FIRST_NAME = "foo";
	private static final String LAST_NAME = "bar";
	private static final String USER_NAME = "awesome123";
	private static final String PASSWORD = "shhhhh";
	private static final String PORTAL_ENDPOINT = "https://www.synapse.org?";

	private static NewUser createNewUser() {
		NewUser user = new NewUser();
		user.setFirstName(FIRST_NAME);
		user.setLastName(LAST_NAME);
		user.setEmail(EMAIL);
		return user;
	}
	
	@Before
	public void before() {
		manager = new PrincipalManagerImpl();
		
		ReflectionTestUtils.setField(manager, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(manager, "sesClient", mockSynapseEmailService);
		ReflectionTestUtils.setField(manager, "userManager", mockUserManager);
		ReflectionTestUtils.setField(manager, "authManager", mockAuthManager);
		ReflectionTestUtils.setField(manager, "userProfileDAO", mockUserProfileDAO);
		ReflectionTestUtils.setField(manager, "notificationEmailDao", mockNotificationEmailDao);
		ReflectionTestUtils.setField(manager, "tokenGenerator", mockTokenGenerator);

		// create some data
		user = createNewUser();
		now = new Date();
	}
	
	@Test
	public void testValid() {
		// Valid
		assertTrue(manager.isAliasValid("one@gmail.org", AliasType.USER_EMAIL));
		assertTrue(manager.isAliasValid("one", AliasType.USER_NAME));
		assertTrue(manager.isAliasValid("Team Name", AliasType.TEAM_NAME));
		assertTrue(manager.isAliasValid("https://gmail.com/myId", AliasType.USER_OPEN_ID));
		// Invalid
		assertFalse(manager.isAliasValid("bad", AliasType.USER_EMAIL));
		assertFalse(manager.isAliasValid("Has Space", AliasType.USER_NAME));
		assertFalse(manager.isAliasValid("@#$%", AliasType.TEAM_NAME));
		assertFalse(manager.isAliasValid("notAURL", AliasType.USER_OPEN_ID));
	}
	
	@Test
	public void testNotAvailable() {
		String toTest = "007";
		when(mockPrincipalAliasDAO.isAliasAvailable(toTest)).thenReturn(false);
		assertFalse(manager.isAliasAvailable(toTest));
	}
	
	@Test
	public void testAvailable() {
		String toTest = "007";
		when(mockPrincipalAliasDAO.isAliasAvailable(toTest)).thenReturn(true);
		assertTrue(manager.isAliasAvailable(toTest));
	}

	// token is OK 23 hours from now
	@Test
	public void testValidateNOTtooOLDTimestamp() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		EmailValidationSignedToken token = new EmailValidationSignedToken();
		token.setEmail(EMAIL);
		token.setCreatedOn(now);
		token.setHmac("signed");
		PrincipalUtils.validateEmailValidationSignedToken(token, notOutOfDate, mockTokenGenerator);
	}

	// token is not OK 25 hours from now
	@Test(expected=IllegalArgumentException.class)
	public void testValidateOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		EmailValidationSignedToken token = new EmailValidationSignedToken();
		token.setEmail(EMAIL);
		token.setCreatedOn(now);
		token.setHmac("signed");
		PrincipalUtils.validateEmailValidationSignedToken(token, outOfDate, mockTokenGenerator);
	}

	@Test
	public void testValidateNonNullUserId() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		EmailValidationSignedToken token = new EmailValidationSignedToken();
		token.setUserId(Long.toString(USER_ID));
		token.setEmail(EMAIL);
		token.setCreatedOn(now);
		token.setHmac("signed");
		try {
			// Method under test
			PrincipalUtils.validateEmailValidationSignedToken(token, notOutOfDate, mockTokenGenerator);
			fail();
		} catch (IllegalArgumentException e) {
			// As expected
		}
	}

	@Test
	public void testNewAccountEmailValidationHappyPath() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(mockSynapseEmailService).sendRawEmail(argument.capture());
		SendRawEmailRequest emailRequest =  argument.getValue();
		assertEquals(Collections.singletonList(EMAIL), emailRequest.getDestinations());
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(emailRequest.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertNotNull(mimeMessage.getSubject());
		// check that all template fields have been replaced
		assertTrue(!body.contains("#"));
		// check that token appears
		assertTrue(body.contains(PORTAL_ENDPOINT));
		assertTrue(body.contains(SerializationUtils.serializeAndHexEncode(PrincipalUtils.createAccountCreationToken(user, now, mockTokenGenerator))));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationMissingFName() throws Exception {
		user.setFirstName(null);
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationMissingLName() throws Exception {
		user.setLastName(null);
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationBogusEmail() throws Exception {
		user.setEmail("invalid-email");
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationInvalidEndpoint() throws Exception {
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationEmailTaken() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
	}

	@Test
	public void testCreateNewAccount() throws Exception {
		AccountSetupInfo accountSetupInfo = new AccountSetupInfo();
		EmailValidationSignedToken emailValidationSignedToken = new EmailValidationSignedToken();
		emailValidationSignedToken.setEmail(user.getEmail());
		emailValidationSignedToken.setCreatedOn(now);
		emailValidationSignedToken.setHmac("signed");
		accountSetupInfo.setEmailValidationSignedToken(emailValidationSignedToken);
		accountSetupInfo.setFirstName(FIRST_NAME);
		accountSetupInfo.setLastName(LAST_NAME);
		accountSetupInfo.setPassword(PASSWORD);
		accountSetupInfo.setUsername(USER_NAME);
		when(mockUserManager.createUser((NewUser)any())).thenReturn(USER_ID);
		manager.createNewAccount(accountSetupInfo);
		ArgumentCaptor<NewUser> newUserCaptor = ArgumentCaptor.forClass(NewUser.class);
		verify(mockUserManager).createUser(newUserCaptor.capture());
		NewUser user = newUserCaptor.getValue();
		assertEquals(FIRST_NAME, user.getFirstName());
		assertEquals(LAST_NAME, user.getLastName());
		assertEquals(USER_NAME, user.getUserName());
		assertEquals(EMAIL, user.getEmail());
		verify(mockAuthManager).changePassword(USER_ID, PASSWORD);
		verify(mockAuthManager).login(USER_ID, PASSWORD, null);
	}

	// token is OK 23 hours from now
	@Test
	public void testValidateAdditionalEmailNOTtooOLDTimestamp() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		EmailValidationSignedToken token = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);
		PrincipalUtils.validateAdditionalEmailSignedToken(token, USER_ID.toString(), notOutOfDate, mockTokenGenerator);
	}

	// token is not OK 25 hours from now
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		EmailValidationSignedToken token = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);
		PrincipalUtils.validateAdditionalEmailSignedToken(token, USER_ID.toString(), outOfDate, mockTokenGenerator);
	}

	@Test
	public void testAdditionalEmailValidation() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		UserProfile profile = new UserProfile();
		profile.setFirstName(FIRST_NAME);
		profile.setLastName(LAST_NAME);
		when(mockUserProfileDAO.get(USER_ID.toString())).thenReturn(profile);
		when(mockPrincipalAliasDAO.getUserName(USER_ID)).thenReturn(USER_NAME);
		manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(mockSynapseEmailService).sendRawEmail(argument.capture());
		SendRawEmailRequest emailRequest =  argument.getValue();
		assertEquals(Collections.singletonList(EMAIL), emailRequest.getDestinations());
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(emailRequest.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertEquals("Request to add or change new email", mimeMessage.getSubject());
		// check that all template fields have been replaced
		assertTrue(!body.contains("#"));
		// check that user's name appears
		assertTrue(body.contains(FIRST_NAME));
		assertTrue(body.contains(LAST_NAME));
		assertTrue(body.contains(USER_NAME));
		assertTrue(body.contains(EMAIL));
		// check that token appears
		assertTrue(body.contains(PORTAL_ENDPOINT));
		assertTrue(body.contains(SerializationUtils.serializeAndHexEncode(PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator))));
	}

	@Test(expected=NameConflictException.class)
	public void testAdditionalEmailEmailAlreadyUsed() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);
		// the following line simulates that the email is already used
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAdditionalEmailValidationAnonymous() throws Exception {
		Long anonId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		UserInfo userInfo = new UserInfo(false, anonId);
		Username email = new Username();
		email.setEmail(EMAIL);
		manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
	}	
	
	@Test(expected=IllegalArgumentException.class)
	public void testAdditionalEmailValidationInvalidEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail("not-an-email-address");
		manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
	}	
	
	@Test(expected=IllegalArgumentException.class)
	public void testAdditionalEmailValidationInvalidEndpoint() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);

		manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
	}	
	
	@Test
	public void testAddEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);

		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);

		Boolean setAsNotificationEmail = true;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);

		ArgumentCaptor<PrincipalAlias> aliasCaptor = ArgumentCaptor.forClass(PrincipalAlias.class);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(aliasCaptor.capture());
		PrincipalAlias alias = aliasCaptor.getValue();
		assertEquals(USER_ID, alias.getPrincipalId());
		assertEquals(AliasType.USER_EMAIL, alias.getType());
		assertEquals(EMAIL, alias.getAlias());
		verify(mockNotificationEmailDao).update((PrincipalAlias)any());
	}
	
	@Test
	public void testAddEmailNoSetNotification() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		
		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);

		Boolean setAsNotificationEmail = null;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);
		
		ArgumentCaptor<PrincipalAlias> aliasCaptor = ArgumentCaptor.forClass(PrincipalAlias.class);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(aliasCaptor.capture());
		PrincipalAlias alias = aliasCaptor.getValue();
		assertEquals(USER_ID, alias.getPrincipalId());
		assertEquals(AliasType.USER_EMAIL, alias.getType());
		assertEquals(EMAIL, alias.getAlias());
		verify(mockNotificationEmailDao, times(0)).update((PrincipalAlias)any());
		
		// null and false are equivalent for this param
		setAsNotificationEmail = false;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);
		verify(mockNotificationEmailDao, times(0)).update((PrincipalAlias)any());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddEmailWrongUser() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(222L, EMAIL, now, mockTokenGenerator);
		manager.addEmail(userInfo, emailValidationSignedToken, null);
	}
	
	@Test
	public void testRemoveEmailHappyCase() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias("notification@mail.com");
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(currentNotificationAlias.getAlias());
		PrincipalAlias alternateEmailAlias = new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		alternateEmailAlias.setAliasId(2L);
		alternateEmailAlias.setPrincipalId(USER_ID);
		alternateEmailAlias.setType(AliasType.USER_EMAIL);
		
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, "notification@mail.com")).
			thenReturn(Collections.singletonList(currentNotificationAlias));
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL,EMAIL)).
			thenReturn(Collections.singletonList(alternateEmailAlias));

		manager.removeEmail(userInfo, EMAIL);
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(USER_ID);
		verify(mockPrincipalAliasDAO).removeAliasFromPrincipal(USER_ID, 2L);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRemoveNotificationEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(currentNotificationAlias.getAlias());
		List<PrincipalAlias> aliases = Collections.singletonList(currentNotificationAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, EMAIL)).thenReturn(aliases);

		manager.removeEmail(userInfo, EMAIL);
	}
	
	@Test(expected=NotFoundException.class)
	public void testRemoveBOGUSEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias("notification@mail.com");
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(currentNotificationAlias.getAlias());
		List<PrincipalAlias> aliases = Collections.singletonList(currentNotificationAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, "notification@mail.com")).thenReturn(aliases);

		manager.removeEmail(userInfo, "bogus@email.com");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetPrincipalIDWithNullRequest() {
		manager.lookupPrincipalId(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetPrincipalIDWithNullAlias() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setType(AliasType.USER_NAME);
		manager.lookupPrincipalId(request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetPrincipalIDWithNullType() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("alias");
		manager.lookupPrincipalId(request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetPrincipalIDWithUnsupoortedType() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("alias");
		request.setType(AliasType.TEAM_NAME);
		manager.lookupPrincipalId(request);
	}

	@Test
	public void testGetPrincipalID() {
		String alias = "alias";
		AliasType type = AliasType.USER_NAME;
		Long id = 1L;
		when(mockPrincipalAliasDAO.lookupPrincipalID(alias, type)).thenReturn(id);
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias(alias);
		request.setType(type);
		PrincipalAliasResponse response = manager.lookupPrincipalId(request);
		assertNotNull(response);
		assertEquals(id, response.getPrincipalId());
	}

	@Test
	public void testSetNotificationEmail() {
		// Setup
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, EMAIL)).
				thenReturn(Collections.singletonList(currentNotificationAlias));

		// Method under test
		manager.setNotificationEmail(userInfo, EMAIL);

		verify(mockNotificationEmailDao).update(currentNotificationAlias);
	}

	@Test
	public void testSetNonExistentNotificationEmail() {
		// Setup
		UserInfo userInfo = new UserInfo(false, USER_ID);
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, EMAIL)).
				thenReturn(Collections.<PrincipalAlias>emptyList());

		try {
			// Method under test
			manager.setNotificationEmail(userInfo, EMAIL);
			fail();
		} catch (NotFoundException e) {
			// As expected
		}

		verify(mockNotificationEmailDao, never()).update(any(PrincipalAlias.class));
	}

	@Test
	public void testGetNotificationEmail() {
		// Setup
		UserInfo userInfo = new UserInfo(false, USER_ID);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(EMAIL);

		// Method under test
		Username username = manager.getNotificationEmail(userInfo);
		assertEquals(EMAIL, username.getEmail());
	}
}
