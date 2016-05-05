package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthenticationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Username;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.principal.AddEmailInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class PrincipalManagerImplUnitTest {

	private PrincipalAliasDAO mockPrincipalAliasDAO;
	private SynapseEmailService mockSynapseEmailService;
	private UserManager mockUserManager;
	private AuthenticationManager mockAuthManager;
	private UserProfileDAO mockUserProfileDAO;
	private NotificationEmailDAO mockNotificationEmailDao;
	
	private PrincipalManagerImpl manager;

	private NewUser user;
	private DomainType domain;
	private Date now;

	private static final String EMAIL = "foo@bar.com";
	private static final String FIRST_NAME = "foo";
	private static final String LAST_NAME = "bar";
	private static final String USER_NAME = "awesome123";
	private static final String PASSWORD = "shhhhh";
	
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
		
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		ReflectionTestUtils.setField(manager, "principalAliasDAO", mockPrincipalAliasDAO);
		
		mockSynapseEmailService = Mockito.mock(SynapseEmailService.class);
		ReflectionTestUtils.setField(manager, "sesClient", mockSynapseEmailService);
		
		mockUserManager = Mockito.mock(UserManager.class);
		ReflectionTestUtils.setField(manager, "userManager", mockUserManager);
		
		mockAuthManager = Mockito.mock(AuthenticationManager.class);
		ReflectionTestUtils.setField(manager, "authManager", mockAuthManager);
		
		mockUserProfileDAO = Mockito.mock(UserProfileDAO.class);
		ReflectionTestUtils.setField(manager, "userProfileDAO", mockUserProfileDAO);
		
		mockNotificationEmailDao = Mockito.mock(NotificationEmailDAO.class);
		ReflectionTestUtils.setField(manager, "notificationEmailDao", mockNotificationEmailDao);

		// create some data
		user = createNewUser();
		domain = DomainType.SYNAPSE;
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
	
	@Test
	public void testGenerateSignature() {
		String token1 = PrincipalManagerImpl.
				generateSignature("my dog has fleas");
		assertTrue(token1.length()>0);
		// same inputs, same result
		String token2 = PrincipalManagerImpl.
				generateSignature("my dog has fleas");
		assertEquals(token1, token2);
		// different inputs, different token
		String token3 = PrincipalManagerImpl.
				generateSignature("my dog has gnats");
		assertTrue(token3.length()>0);
		assertFalse(token1.equals(token3));
	}
	
	@Test
	public void testGenerateSignatureForNewAccount() {
		String token1 = PrincipalManagerImpl.
				generateSignatureForNewAccount("Foo", "Bar", 
				"foo@bar.com", "2014-Jun-02", "synapse");
		assertTrue(token1.length()>0);
		// same inputs, same result
		String token2 = PrincipalManagerImpl.
				generateSignatureForNewAccount("Foo", "Bar", 
				"foo@bar.com", "2014-Jun-02", "synapse");
		assertEquals(token1, token2);
		// different inputs, different token
		String token3 = PrincipalManagerImpl.
				generateSignatureForNewAccount("Foo", "Bas", 
				"foo@bar.com", "2014-Jun-02", "synapse");
		assertTrue(token3.length()>0);
		assertFalse(token1.equals(token3));
	}
	
	@Test
	public void testCreateTokenForNewAccount() {
		String token1 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, now);
		assertTrue(token1.length()>0);
		// same inputs, same result
		String token2 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, now);
		assertEquals(token1, token2);
		// different inputs, different token
		Date otherDate = new Date(now.getTime()+1L);
		String token3 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, otherDate);
		assertTrue(token3.length()>0);
		assertFalse(token1.equals(token3));
	}
	
	@Test
	public void testCreateTokenForNewAccountNoName() {
		user.setFirstName("");
		user.setLastName("");
		String token1 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, now);
		assertTrue(token1.length()>0);
		// same inputs, same result
		String token2 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, now);
		assertEquals(token1, token2);
		// different inputs, different token
		Date otherDate = new Date(now.getTime()+1L);
		String token3 = PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, otherDate);
		assertTrue(token3.length()>0);
		assertFalse(token1.equals(token3));
	}
	
	@Test
	public void testValidateNewAccountToken() {
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		String extractedEmail = PrincipalManagerImpl.validateNewAccountToken(token, now);
		assertEquals(EMAIL, extractedEmail);
	}
	
	@Test
	public void testValidateNewAccountTokenNoName() {
		user.setFirstName("");
		user.setLastName("");
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		String extractedEmail = PrincipalManagerImpl.validateNewAccountToken(token, now);
		assertEquals(EMAIL, extractedEmail);
	}
	
	private static String paste(String[] pieces, String and) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (String piece : pieces) {
			if (first) first=false; else sb.append(and);
			if (piece!=null) sb.append(piece);
		}
		return sb.toString();
	}
	
	// try removing a required field from the 'token'
	private void testMissingParamValidateNewAccountToken(String paramName) {
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		String[] params = token.split("&");
		for (int i=0; i<params.length; i++) {
			if (params[i].indexOf(paramName)>=0) params[i]=null;
		}
		token = paste(params, "&");
		PrincipalManagerImpl.validateNewAccountToken(token, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingFirstName() {
		testMissingParamValidateNewAccountToken("firstname");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingLastName() {
		testMissingParamValidateNewAccountToken("lastname");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingEmail() {
		testMissingParamValidateNewAccountToken("email");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingTimeStamp() {
		testMissingParamValidateNewAccountToken("timestamp");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingDomain() {
		testMissingParamValidateNewAccountToken("domain");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenMissingMac() {
		testMissingParamValidateNewAccountToken("mac");
	}
	
	// try removing a required field from the 'token'
	private void testReplacedParamValidateNewAccountToken(String paramName, String paramValue) {
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		String[] params = token.split("&");
		for (int i=0; i<params.length; i++) {
			if (params[i].startsWith(paramName)) params[i]=paramName+"="+paramValue;
		}
		token = paste(params, "&");
		PrincipalManagerImpl.validateNewAccountToken(token, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenBadTimestamp() {
		testReplacedParamValidateNewAccountToken("timestamp", "not-a-time-stamp");
	}
	
	// token is OK 23 hours from now
	@Test
	public void testValidateNOTtooOLDTimestamp() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		PrincipalManagerImpl.validateNewAccountToken(token, notOutOfDate);
	}
	
	// token is not OK 25 hours from now
	@Test(expected=IllegalArgumentException.class)
	public void testValidateOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		PrincipalManagerImpl.validateNewAccountToken(token, outOfDate);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateNewAccountTokenInvalidToken() {
		testReplacedParamValidateNewAccountToken("mac", "invalid-mac");
	}
	@Test
	public void testNewAccountEmailValidationHappyPath() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		manager.newAccountEmailValidation(user, "https://www.synapse.org?", DomainType.SYNAPSE);
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSynapseEmailService).sendEmail(argument.capture());
		SendEmailRequest emailRequest =  argument.getValue();
		assertEquals(Collections.singletonList(EMAIL), emailRequest.getDestination().getToAddresses());
		Message message = emailRequest.getMessage();
		assertEquals("Welcome to SYNAPSE!", message.getSubject().getData());
		String body = message.getBody().getHtml().getData();
		// check that all template fields have been replaced
		assertTrue(body.indexOf("#")<0);
		assertTrue(body.indexOf(FIRST_NAME)>=0); 
		// check that user's name appears
		assertTrue(body.indexOf(LAST_NAME)>=0); 
		// check that token appears
		assertTrue(body.indexOf("https://www.synapse.org?")>=0); 
		assertTrue(body.indexOf("firstname=foo&lastname=bar&email=foo%40bar.com")>=0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationMissingFName() throws Exception {
		user.setFirstName(null);
		manager.newAccountEmailValidation(user, "https://www.synapse.org?", DomainType.SYNAPSE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationMissingLName() throws Exception {
		user.setLastName(null);
		manager.newAccountEmailValidation(user, "https://www.synapse.org?", DomainType.SYNAPSE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationBogusEmail() throws Exception {
		user.setEmail("invalid-email");
		manager.newAccountEmailValidation(user, "https://www.synapse.org?", DomainType.SYNAPSE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationInvalidEndpoint() throws Exception {
		manager.newAccountEmailValidation(user, "www.synapse.org", DomainType.SYNAPSE);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNewAccountEmailValidationEmailTaken() throws Exception {
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		manager.newAccountEmailValidation(user, "https://www.synapse.org?", DomainType.SYNAPSE);
	}
	
	@Test
	public void testCreateNewAccount() throws Exception {
		AccountSetupInfo accountSetupInfo = new AccountSetupInfo();
		accountSetupInfo.setEmailValidationToken(
				PrincipalManagerImpl.
				createTokenForNewAccount(user, domain, now));
		accountSetupInfo.setFirstName(FIRST_NAME);
		accountSetupInfo.setLastName(LAST_NAME);
		accountSetupInfo.setPassword(PASSWORD);
		accountSetupInfo.setUsername(USER_NAME);
		long principalId = 111L;
		when(mockUserManager.createUser((NewUser)any())).thenReturn(principalId);
		manager.createNewAccount(accountSetupInfo, DomainType.SYNAPSE);
		ArgumentCaptor<NewUser> newUserCaptor = ArgumentCaptor.forClass(NewUser.class);
		verify(mockUserManager).createUser(newUserCaptor.capture());
		NewUser user = newUserCaptor.getValue();
		assertEquals(FIRST_NAME, user.getFirstName());
		assertEquals(LAST_NAME, user.getLastName());
		assertEquals(USER_NAME, user.getUserName());
		assertEquals(EMAIL, user.getEmail());
		verify(mockAuthManager).changePassword(principalId, PASSWORD);
		verify(mockAuthManager).authenticate(principalId, PASSWORD, DomainType.SYNAPSE);
	}
	
	@Test
	public void testCreateTokenForAdditionalEmail() {
		String token1 = PrincipalManagerImpl.
				createTokenForAdditionalEmail(111L, EMAIL, domain, now);
		assertTrue(token1.length()>0);
		// same inputs, same result
		String token2 = PrincipalManagerImpl.
				createTokenForAdditionalEmail(111L, EMAIL, domain, now);
		assertEquals(token1, token2);
		// different inputs, different token
		String token3 = PrincipalManagerImpl.
				createTokenForAdditionalEmail(111L, "someother@email.com", domain, now);
		assertTrue(token3.length()>0);
		assertFalse(token1.equals(token3));
	}
	
	@Test
	public void testValidateTokenForAdditionalEmail() {
		Long principalId = 111L;
		String token = PrincipalManagerImpl.createTokenForAdditionalEmail(principalId, EMAIL, domain, now);
		PrincipalManagerImpl.validateAdditionalEmailToken(token, now);
		String validatedEmail = PrincipalManagerImpl.getParameterValueFromToken(token, "email");
		String originalUserId = PrincipalManagerImpl.getParameterValueFromToken(token, "userid");
		assertEquals(EMAIL, validatedEmail);
		assertEquals(principalId.toString(), originalUserId);
	}
	
	// try removing a required field from the 'token'
	private void testMissingParamValidateAdditionalEmailToken(String paramName) {
		String token = PrincipalManagerImpl.createTokenForNewAccount(user, domain, now);
		String[] params = token.split("&");
		for (int i=0; i<params.length; i++) {
			if (params[i].indexOf(paramName)>=0) params[i]=null;
		}
		token = paste(params, "&");
		PrincipalManagerImpl.validateAdditionalEmailToken(token, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenMissingFirstName() {
		testMissingParamValidateAdditionalEmailToken("userid");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenMissingEmail() {
		testMissingParamValidateAdditionalEmailToken("email");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenMissingTimeStamp() {
		testMissingParamValidateAdditionalEmailToken("timestamp");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidatAdditionalEmailTokenMissingDomain() {
		testMissingParamValidateAdditionalEmailToken("domain");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenMissingMac() {
		testMissingParamValidateAdditionalEmailToken("mac");
	}
	
	// try removing a required field from the 'token'
	private void testReplacedParamValidateAdditionalEmailToken(String paramName, String paramValue) {
		String token = PrincipalManagerImpl.createTokenForAdditionalEmail(111L, EMAIL, domain, now);
		String[] params = token.split("&");
		for (int i=0; i<params.length; i++) {
			if (params[i].startsWith(paramName)) params[i]=paramName+"="+paramValue;
		}
		token = paste(params, "&");
		PrincipalManagerImpl.validateAdditionalEmailToken(token, now);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenBadTimestamp() {
		testReplacedParamValidateAdditionalEmailToken("timestamp", "not-a-time-stamp");
	}
	
	// token is OK 23 hours from now
	@Test
	public void testValidateAdditionalEmailNOTtooOLDTimestamp() {
		Date notOutOfDate = new Date(System.currentTimeMillis()+23*3600*1000L);
		String token = PrincipalManagerImpl.createTokenForAdditionalEmail(111L, EMAIL, domain, now);
		PrincipalManagerImpl.validateAdditionalEmailToken(token, notOutOfDate);
	}
	
	// token is not OK 25 hours from now
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailOLDTimestamp() {
		Date outOfDate = new Date(System.currentTimeMillis()+25*3600*1000L);
		String token = PrincipalManagerImpl.createTokenForAdditionalEmail(111L, EMAIL, domain, now);
		PrincipalManagerImpl.validateAdditionalEmailToken(token, outOfDate);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateAdditionalEmailTokenInvalidToken() {
		testReplacedParamValidateAdditionalEmailToken("mac", "invalid-mac");
	}
	
	@Test
	public void testAdditionalEmailValidation() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		String portalEndpoint = "https://www.synapse.org?";
		Username email = new Username();
		email.setEmail(EMAIL);
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(true);
		UserProfile profile = new UserProfile();
		profile.setFirstName(FIRST_NAME);
		profile.setLastName(LAST_NAME);
		when(mockUserProfileDAO.get(principalId.toString())).thenReturn(profile);
		when(mockPrincipalAliasDAO.getUserName(principalId)).thenReturn(USER_NAME);
		
		manager.additionalEmailValidation(userInfo, email, portalEndpoint, domain);
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(mockSynapseEmailService).sendEmail(argument.capture());
		SendEmailRequest emailRequest =  argument.getValue();
		assertEquals(Collections.singletonList(EMAIL), emailRequest.getDestination().getToAddresses());
		Message message = emailRequest.getMessage();
		assertEquals("Request to add or change new email", message.getSubject().getData());
		String body = message.getBody().getHtml().getData();
		// check that all template fields have been replaced
		assertTrue(body, body.indexOf("#")<0);
		// check that user's name appears
		assertTrue(body.indexOf(FIRST_NAME)>=0); 
		assertTrue(body.indexOf(LAST_NAME)>=0); 
		assertTrue(body.indexOf(USER_NAME)>=0); 
		assertTrue(body.indexOf(EMAIL)>=0); 
		assertTrue(body.indexOf(domain.name())>=0); 
		// check that token appears
		assertTrue(body.indexOf("https://www.synapse.org?")>=0); 
		assertTrue(body.indexOf("userid=111&email=foo%40bar.com&timestamp=")>=0);
	}

	@Test(expected=NameConflictException.class)
	public void testAdditionalEmailEmailAlreadyUsed() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		String portalEndpoint = "https://www.synapse.org?";
		Username email = new Username();
		email.setEmail(EMAIL);
		// the following line simulates that the email is already used
		when(mockPrincipalAliasDAO.isAliasAvailable(EMAIL)).thenReturn(false);
		manager.additionalEmailValidation(userInfo, email, portalEndpoint, domain);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAdditionalEmailValidationAnonymous() throws Exception {
		Long principalId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		UserInfo userInfo = new UserInfo(false, principalId);
		Username email = new Username();
		email.setEmail(EMAIL);
		String portalEndpoint = "https://www.synapse.org?";
		manager.additionalEmailValidation(userInfo, email, portalEndpoint, domain);
	}	
	
	@Test(expected=IllegalArgumentException.class)
	public void testAdditionalEmailValidationInvalidEmail() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		Username email = new Username();
		email.setEmail("not-an-email-address");
		String portalEndpoint = "https://www.synapse.org?";
		manager.additionalEmailValidation(userInfo, email, portalEndpoint, domain);
	}	
	
	@Test(expected=IllegalArgumentException.class)
	public void testAdditionalEmailValidationInvalidEndpoint() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		Username email = new Username();
		email.setEmail(EMAIL);
		String portalEndpoint = "www.synapse.org"; // not a valid endpoint!

		manager.additionalEmailValidation(userInfo, email, portalEndpoint, domain);
	}	
	
	@Test
	public void testAddEmail() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		
		String emailValidationToken = PrincipalManagerImpl.createTokenForAdditionalEmail(principalId, EMAIL, domain, now);
		AddEmailInfo addEmailInfo = new AddEmailInfo();
		addEmailInfo.setEmailValidationToken(emailValidationToken);
		
		Boolean setAsNotificationEmail = true;
		manager.addEmail(userInfo, addEmailInfo, setAsNotificationEmail);
		
		ArgumentCaptor<PrincipalAlias> aliasCaptor = ArgumentCaptor.forClass(PrincipalAlias.class);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(aliasCaptor.capture());
		PrincipalAlias alias = aliasCaptor.getValue();
		assertEquals(principalId, alias.getPrincipalId());
		assertEquals(AliasType.USER_EMAIL, alias.getType());
		assertEquals(EMAIL, alias.getAlias());
		verify(mockNotificationEmailDao).update((PrincipalAlias)any());
	}
	
	@Test
	public void testAddEmailNoSetNotification() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		
		String emailValidationToken = PrincipalManagerImpl.createTokenForAdditionalEmail(principalId, EMAIL, domain, now);
		AddEmailInfo addEmailInfo = new AddEmailInfo();
		addEmailInfo.setEmailValidationToken(emailValidationToken);
		
		Boolean setAsNotificationEmail = null;
		manager.addEmail(userInfo, addEmailInfo, setAsNotificationEmail);
		
		ArgumentCaptor<PrincipalAlias> aliasCaptor = ArgumentCaptor.forClass(PrincipalAlias.class);
		verify(mockPrincipalAliasDAO).bindAliasToPrincipal(aliasCaptor.capture());
		PrincipalAlias alias = aliasCaptor.getValue();
		assertEquals(principalId, alias.getPrincipalId());
		assertEquals(AliasType.USER_EMAIL, alias.getType());
		assertEquals(EMAIL, alias.getAlias());
		verify(mockNotificationEmailDao, times(0)).update((PrincipalAlias)any());
		
		// null and false are equivalent for this param
		setAsNotificationEmail = false;
		manager.addEmail(userInfo, addEmailInfo, setAsNotificationEmail);
		verify(mockNotificationEmailDao, times(0)).update((PrincipalAlias)any());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddEmailWrongUser() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		
		String emailValidationToken = PrincipalManagerImpl.createTokenForAdditionalEmail(222L, EMAIL, domain, now);
		AddEmailInfo addEmailInfo = new AddEmailInfo();
		addEmailInfo.setEmailValidationToken(emailValidationToken);
		
		manager.addEmail(userInfo, addEmailInfo, null);
	}
	
	@Test
	public void testRemoveEmailHappyCase() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias("notification@mail.com");
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(principalId);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(principalId)).thenReturn(currentNotificationAlias.getAlias());
		PrincipalAlias alternateEmailAlias = new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		alternateEmailAlias.setAliasId(2L);
		alternateEmailAlias.setPrincipalId(principalId);
		alternateEmailAlias.setType(AliasType.USER_EMAIL);
		
		when(mockPrincipalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL, "notification@mail.com")).
			thenReturn(Collections.singletonList(currentNotificationAlias));
		when(mockPrincipalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL,EMAIL)).
			thenReturn(Collections.singletonList(alternateEmailAlias));

		manager.removeEmail(userInfo, EMAIL);
		verify(mockNotificationEmailDao).getNotificationEmailForPrincipal(principalId);
		verify(mockPrincipalAliasDAO).removeAliasFromPrincipal(principalId, 2L);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRemoveNotificationEmail() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias(EMAIL);
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(principalId);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(principalId)).thenReturn(currentNotificationAlias.getAlias());
		List<PrincipalAlias> aliases = Collections.singletonList(currentNotificationAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL, EMAIL)).thenReturn(aliases);

		manager.removeEmail(userInfo, EMAIL);
	}
	
	@Test(expected=NotFoundException.class)
	public void testRemoveBOGUSEmail() throws Exception {
		Long principalId = 111L;
		UserInfo userInfo = new UserInfo(false, principalId);
		PrincipalAlias currentNotificationAlias =  new PrincipalAlias();
		currentNotificationAlias.setAlias("notification@mail.com");
		Long aliasId = 1L;
		currentNotificationAlias.setAliasId(aliasId);
		currentNotificationAlias.setPrincipalId(principalId);
		currentNotificationAlias.setType(AliasType.USER_EMAIL);
		when(mockNotificationEmailDao.getNotificationEmailForPrincipal(principalId)).thenReturn(currentNotificationAlias.getAlias());
		List<PrincipalAlias> aliases = Collections.singletonList(currentNotificationAlias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(principalId, AliasType.USER_EMAIL, "notification@mail.com")).thenReturn(aliases);

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
}
