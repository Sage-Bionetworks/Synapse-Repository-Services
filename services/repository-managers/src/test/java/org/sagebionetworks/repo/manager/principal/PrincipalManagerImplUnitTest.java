package org.sagebionetworks.repo.manager.principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.EmailQuarantineReason;
import org.sagebionetworks.repo.model.principal.EmailQuarantineStatus;
import org.sagebionetworks.repo.model.principal.EmailValidationSignedToken;
import org.sagebionetworks.repo.model.principal.NotificationEmail;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.SerializationUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@ExtendWith(MockitoExtension.class)
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
	private EmailQuarantineDao mockEmailQuarantineDao;
	@Mock
	private TokenGenerator mockTokenGenerator;
	@Mock
	private QuarantinedEmail mockQuarantinedEmail;
	
	@InjectMocks
	private PrincipalManagerImpl manager;

	private NewUser user;
	private Date now;

	private UserInfo adminUserInfo;

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
	
	@BeforeEach
	public void before() {
		// create some data
		user = createNewUser();
		now = new Date();

		adminUserInfo = new UserInfo(true);
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
	@Test
	public void testValidateOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		EmailValidationSignedToken token = new EmailValidationSignedToken();
		token.setEmail(EMAIL);
		token.setCreatedOn(now);
		token.setHmac("signed");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			PrincipalUtils.validateEmailValidationSignedToken(token, outOfDate, mockTokenGenerator);
		});
	}

	@Test
	public void testValidateNonNullUserId() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		EmailValidationSignedToken token = new EmailValidationSignedToken();
		token.setUserId(Long.toString(USER_ID));
		token.setEmail(EMAIL);
		token.setCreatedOn(now);
		token.setHmac("signed");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			// Method under test
			PrincipalUtils.validateEmailValidationSignedToken(token, notOutOfDate, mockTokenGenerator);
		});
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
	
	@Test
	public void testNewAccountEmailValidationMissingFName() throws Exception {
		user.setFirstName(null);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});
	}
	
	@Test
	public void testNewAccountEmailValidationMissingLName() throws Exception {
		user.setLastName(null);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});
	}
	
	@Test
	public void testNewAccountEmailValidationBogusEmail() throws Exception {
		user.setEmail("invalid-email");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});
	}
	
	@Test
	public void testNewAccountEmailValidationInvalidEndpoint() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});
	}

	@Test
	public void testNewAccountEmailValidationEmailTaken() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});
	}
	
	@Test
	public void testNewAccountEmailValidationWithQuarantinedAddress() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		when(mockEmailQuarantineDao.isQuarantined(EMAIL)).thenReturn(true);
		
		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {
			manager.newAccountEmailValidation(user, PORTAL_ENDPOINT, now);
		});

		verify(mockEmailQuarantineDao).isQuarantined(EMAIL);
		verifyZeroInteractions(mockSynapseEmailService);
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
		verify(mockAuthManager).setPassword(USER_ID, PASSWORD);
		verify(mockAuthManager).loginWithNoPasswordCheck(USER_ID);
	}

	// token is OK 23 hours from now
	@Test
	public void testValidateAdditionalEmailNOTtooOLDTimestamp() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		EmailValidationSignedToken token = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);
		PrincipalUtils.validateAdditionalEmailSignedToken(token, USER_ID.toString(), notOutOfDate, mockTokenGenerator);
	}

	// token is not OK 25 hours from now
	@Test
	public void testValidateAdditionalEmailOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		EmailValidationSignedToken token = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			PrincipalUtils.validateAdditionalEmailSignedToken(token, USER_ID.toString(), outOfDate, mockTokenGenerator);
		});
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
	
	@Test
	public void testAdditionalEmailWithQuarantinedAddress() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);
		
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		when(mockEmailQuarantineDao.isQuarantined(EMAIL)).thenReturn(true);
		
		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {			
			manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		});
	
		verify(mockEmailQuarantineDao).isQuarantined(EMAIL);
		verifyZeroInteractions(mockUserProfileDAO);
		verifyZeroInteractions(mockSynapseEmailService);
		
	}

	@Test
	public void testAdditionalEmailEmailAlreadyUsed() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);
		// the following line simulates that the email is already used
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		
		Assertions.assertThrows(NameConflictException.class, ()-> {	
			manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		});
	}
	
	@Test
	public void testAdditionalEmailValidationAnonymous() throws Exception {
		Long anonId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		UserInfo userInfo = new UserInfo(false, anonId);
		Username email = new Username();
		email.setEmail(EMAIL);
		Assertions.assertThrows(UnauthorizedException.class, ()-> {	
			manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		});
	}	
	
	@Test
	public void testAdditionalEmailValidationInvalidEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail("not-an-email-address");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		});
	}	
	
	@Test
	public void testAdditionalEmailValidationInvalidEndpoint() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		Username email = new Username();
		email.setEmail(EMAIL);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.additionalEmailValidation(userInfo, email, PORTAL_ENDPOINT, now);
		});
	}	
	
	@Test
	public void testAddEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);

		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);

		PrincipalAlias expectedAlias = new PrincipalAlias();
		
		expectedAlias.setAlias(EMAIL);
		expectedAlias.setPrincipalId(USER_ID);
		expectedAlias.setType(AliasType.USER_EMAIL);
		
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(expectedAlias)).thenReturn(expectedAlias);
		
		Boolean setAsNotificationEmail = true;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);

		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(expectedAlias);
		verify(mockNotificationEmailDao).update((PrincipalAlias)any());
	}
	
	@Test
	public void testAddEmailNoSetNotification() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		
		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(USER_ID, EMAIL, now, mockTokenGenerator);

		PrincipalAlias expectedAlias = new PrincipalAlias();
		
		expectedAlias.setAlias(EMAIL);
		expectedAlias.setPrincipalId(USER_ID);
		expectedAlias.setType(AliasType.USER_EMAIL);
		
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(expectedAlias)).thenReturn(expectedAlias);
		
		Boolean setAsNotificationEmail = null;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);
		
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(expectedAlias);
		verifyZeroInteractions(mockNotificationEmailDao);
		
		// null and false are equivalent for this param
		setAsNotificationEmail = false;
		manager.addEmail(userInfo, emailValidationSignedToken, setAsNotificationEmail);
		
		verifyZeroInteractions(mockNotificationEmailDao);
	}
	
	@Test
	public void testAddEmailWrongUser() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		EmailValidationSignedToken emailValidationSignedToken = PrincipalUtils.createEmailValidationSignedToken(222L, EMAIL, now, mockTokenGenerator);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.addEmail(userInfo, emailValidationSignedToken, null);
		});
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
		
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, EMAIL)).
			thenReturn(Collections.singletonList(alternateEmailAlias));

		manager.removeEmail(userInfo, EMAIL);
		
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(USER_ID);
		verify(mockPrincipalAliasDAO).removeAliasFromPrincipal(USER_ID, 2L);
	}

	@Test
	public void testRemoveNotificationEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(currentNotificationAlias.getAlias());

		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.removeEmail(userInfo, EMAIL);
		});
	}
	
	@Test
	public void testRemoveBOGUSEmail() throws Exception {
		UserInfo userInfo = new UserInfo(false, USER_ID);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias("notification@mail.com");
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(USER_ID);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(currentNotificationAlias.getAlias());
		when(mockPrincipalAliasDAO.listPrincipalAliases(USER_ID, AliasType.USER_EMAIL, "bogus@email.com")).thenReturn(Collections.emptyList());

		Assertions.assertThrows(NotFoundException.class, ()-> {	
			manager.removeEmail(userInfo, "bogus@email.com");
		});
	}

	@Test
	public void testGetPrincipalIDWithNullRequest() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.lookupPrincipalId(null);
		});
	}

	@Test
	public void testGetPrincipalIDWithNullAlias() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setType(AliasType.USER_NAME);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.lookupPrincipalId(request);
		});
	}

	@Test
	public void testGetPrincipalIDWithNullType() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("alias");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.lookupPrincipalId(request);
		});
	}

	@Test
	public void testGetPrincipalIDWithUnsupoortedType() {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias("alias");
		request.setType(AliasType.TEAM_NAME);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {	
			manager.lookupPrincipalId(request);
		});
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

		Assertions.assertThrows(NotFoundException.class, ()-> {	
			// Method under test
			manager.setNotificationEmail(userInfo, EMAIL);
		});

		verify(mockNotificationEmailDao, never()).update(any(PrincipalAlias.class));
		verifyZeroInteractions(mockEmailQuarantineDao);
	}

	@Test
	public void testGetNotificationEmail() {
		// Setup
		UserInfo userInfo = new UserInfo(false, USER_ID);
		
		when(mockEmailQuarantineDao.getQuarantinedEmail(EMAIL)).thenReturn(Optional.empty());
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(EMAIL);

		NotificationEmail expected = new NotificationEmail();
		expected.setEmail(EMAIL);
		
		// Method under test
		NotificationEmail result = manager.getNotificationEmail(userInfo);
		
		assertEquals(expected, result);
		
		verify(mockEmailQuarantineDao).getQuarantinedEmail(EMAIL);
		verifyNoMoreInteractions(mockEmailQuarantineDao);
	}
	
	@Test
	public void testGetNotificationEmailWithQuarantineStatus() {
		// Setup
		UserInfo userInfo = new UserInfo(false, USER_ID);

		EmailQuarantineReason quarantineReason = EmailQuarantineReason.PERMANENT_BOUNCE;
		
		when(mockQuarantinedEmail.getReason()).thenReturn(quarantineReason);
		when(mockEmailQuarantineDao.getQuarantinedEmail(EMAIL)).thenReturn(Optional.of(mockQuarantinedEmail));
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(USER_ID)).thenReturn(EMAIL);

		NotificationEmail expected = new NotificationEmail();
		expected.setEmail(EMAIL);
		
		EmailQuarantineStatus quarantineStatus = new EmailQuarantineStatus();
		quarantineStatus.setReason(quarantineReason);
		
		expected.setQuarantineStatus(quarantineStatus);
		
		// Method under test
		NotificationEmail result = manager.getNotificationEmail(userInfo);
		
		assertEquals(expected, result);
		
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(USER_ID);
		verify(mockEmailQuarantineDao).getQuarantinedEmail(EMAIL);
		verify(mockQuarantinedEmail).getReason();
		verify(mockQuarantinedEmail).getReasonDetails();
		verify(mockQuarantinedEmail).getExpiresOn();
		verifyNoMoreInteractions(mockEmailQuarantineDao);
	}

	@Test
	public void clearUserInformationSuccess() {
		String expectedEmail = "gdpr-synapse+" + USER_ID + "@sagebase.org";

		PrincipalAlias expectedEmailAlias = new PrincipalAlias();
		expectedEmailAlias.setPrincipalId(USER_ID);
		expectedEmailAlias.setAlias(expectedEmail);
		expectedEmailAlias.setType(AliasType.USER_EMAIL);

		UserProfile expectedProfile = new UserProfile();
		expectedProfile.setEmail(expectedEmail);
		expectedProfile.setEmails(Collections.singletonList(expectedEmail));
		expectedProfile.setFirstName("");
		expectedProfile.setLastName("");
		expectedProfile.setOpenIds(Collections.emptyList());
		expectedProfile.setUserName(USER_ID.toString());
		expectedProfile.setOwnerId(USER_ID.toString());
		Settings notificationSettings = new Settings();
		notificationSettings.setSendEmailNotifications(false);
		expectedProfile.setNotificationSettings(notificationSettings);
		expectedProfile.setDisplayName(null);
		expectedProfile.setIndustry(null);
		expectedProfile.setProfilePicureFileHandleId(null);
		expectedProfile.setLocation(null);
		expectedProfile.setCompany(null);
		expectedProfile.setPosition(null);


		when(mockPrincipalAliasDAO.removeAllAliasFromPrincipal(USER_ID)).thenReturn(true);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(expectedEmailAlias)).thenReturn(expectedEmailAlias);
		doNothing().when(mockNotificationEmailDao).update(expectedEmailAlias);
		when(mockUserProfileDAO.get(USER_ID.toString())).thenReturn(new UserProfile());
		when(mockUserProfileDAO.update(expectedProfile)).thenReturn(expectedProfile);
		doNothing().when(mockAuthManager).setPassword(eq(USER_ID), anyString());

		// Method under test
		manager.clearPrincipalInformation(adminUserInfo, USER_ID);


		verify(mockPrincipalAliasDAO).removeAllAliasFromPrincipal(USER_ID);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(expectedEmailAlias);
		verify(mockNotificationEmailDao).update(expectedEmailAlias);
		verify(mockUserProfileDAO).update(any(UserProfile.class));
		verify(mockAuthManager).setPassword(eq(USER_ID), anyString());
	}

	@Test
	public void clearUserInformationNonAdmin() {
		assertThrows(UnauthorizedException.class, () -> manager.clearPrincipalInformation(new UserInfo(false), USER_ID));
	}


	@Test
	public void clearUserInformationNullUserId() {
		// Method under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.clearPrincipalInformation(adminUserInfo,null));

	}

	@Test
	public void clearUserInformationNoAliasesRemoved() {
		when(mockPrincipalAliasDAO.removeAllAliasFromPrincipal(USER_ID)).thenReturn(false);
		// Method under test
		assertThrows(DatastoreException.class,
				() -> manager.clearPrincipalInformation(adminUserInfo, USER_ID),
				"Removed zero aliases from principal: " + USER_ID + ". A principal record should have at least one alias.");
	}
}
