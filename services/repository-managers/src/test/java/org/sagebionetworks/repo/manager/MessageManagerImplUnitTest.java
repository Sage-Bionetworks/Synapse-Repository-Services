package org.sagebionetworks.repo.manager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.util.MessageTestUtil;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.SerializationUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;


public class MessageManagerImplUnitTest {
	private MessageManagerImpl messageManager;
	@Mock
	private UserManager userManager;
	@Mock
	private MessageDAO messageDAO;
	@Mock
	private UserGroupDAO userGroupDAO;
	@Mock
	private GroupMembersDAO groupMembersDao;
	@Mock
	private UserProfileManager userProfileManager;
	@Mock
	private NotificationEmailDAO notificationEmailDao;
	@Mock
	private PrincipalAliasDAO principalAliasDAO;
	@Mock
	private AuthorizationManager authorizationManager;
	@Mock
	private FileHandleDao fileHandleDAO;
	@Mock
	private NodeDAO nodeDAO;
	@Mock
	private EntityPermissionsManager entityPermissionsManager;
	@Mock
	private SynapseEmailService sesClient;
	@Mock
	private FileHandleManager fileHandleManager;
	
	private MessageToUser mtu;
	private FileHandle fileHandle;
	
	private static final String MESSAGE_ID = "101";
	private static final Long CREATOR_ID = 999L;
	private static final String CREATOR_EMAIL = "foo@sagebase.org";
	private static final Long RECIPIENT_ID = 888L;
	private static final String RECIPIENT_EMAIL = "bar@sagebase.org";
	private static final String RECIPIENT_EMAIL_ALIAS = "bar@alternative.org";
	private static final String FILE_HANDLE_ID = "222";
	private static final String UNSUBSCRIBE_ENDPOINT = "https://www.synapse.org/#unsub:";
	private static final String PROFILE_SETTING_ENDPOINT = "https://www.synapse.org/#profile:edit";
	private UserInfo creatorUserInfo = null;
	private PrincipalAlias recipientUsernameAlias = null;
	private PrincipalAlias recipientEmailAlias = null;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		messageManager = new MessageManagerImpl();
		ReflectionTestUtils.setField(messageManager, "messageDAO", messageDAO);
		ReflectionTestUtils.setField(messageManager, "userGroupDAO", userGroupDAO);
		ReflectionTestUtils.setField(messageManager, "groupMembersDAO", groupMembersDao);
		ReflectionTestUtils.setField(messageManager, "userManager", userManager);
		ReflectionTestUtils.setField(messageManager, "userProfileManager", userProfileManager);
		ReflectionTestUtils.setField(messageManager, "notificationEmailDao", notificationEmailDao);
		ReflectionTestUtils.setField(messageManager, "principalAliasDAO", principalAliasDAO);
		ReflectionTestUtils.setField(messageManager, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(messageManager, "fileHandleDao", fileHandleDAO);
		ReflectionTestUtils.setField(messageManager, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(messageManager, "entityPermissionsManager", entityPermissionsManager);
		ReflectionTestUtils.setField(messageManager, "sesClient", sesClient);
		ReflectionTestUtils.setField(messageManager, "fileHandleManager", fileHandleManager);
		
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		
		creatorUserInfo = new UserInfo(false);
		creatorUserInfo.setId(CREATOR_ID);
		creatorUserInfo.setGroups(Collections.singleton(CREATOR_ID));
		
		recipientUsernameAlias = new PrincipalAlias();
		recipientUsernameAlias.setAlias("bar");
		recipientUsernameAlias.setPrincipalId(RECIPIENT_ID);
		recipientUsernameAlias.setType(AliasType.USER_NAME);
		
		recipientEmailAlias = new PrincipalAlias();
		recipientEmailAlias.setAlias(RECIPIENT_EMAIL_ALIAS);
		recipientEmailAlias.setPrincipalId(RECIPIENT_ID);
		recipientEmailAlias.setType(AliasType.USER_EMAIL);
		
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(creatorUserInfo);
		
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn(CREATOR_EMAIL);
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		
		{
			UserProfile userProfile = new UserProfile();
			userProfile.setFirstName("Foo");
			userProfile.setLastName("FOO");
			userProfile.setOwnerId(CREATOR_ID.toString());
			userProfile.setUserName("foo");
			when(userProfileManager.getUserProfile(CREATOR_ID.toString())).thenReturn(userProfile);
		}
		
		{
			UserProfile userProfile = new UserProfile();
			userProfile.setFirstName("Bar");
			userProfile.setLastName("BAR");
			userProfile.setOwnerId(RECIPIENT_ID.toString());
			userProfile.setUserName("bar");
			when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfile);
		}
		
		
		when(messageDAO.canCreateMessage(eq(CREATOR_ID.toString()), 
				anyLong(), anyLong())).thenReturn(true);
		when(authorizationManager.canAccessRawFileHandleById(creatorUserInfo, FILE_HANDLE_ID)).
			thenReturn(AuthorizationStatus.authorized());
		UserGroup ug = new UserGroup();
		ug.setId(RECIPIENT_ID.toString());
		ug.setIsIndividual(true);
		when(userGroupDAO.get(eq(RECIPIENT_ID))).thenReturn(ug);
		when(userGroupDAO.get(eq(Collections.singletonList(RECIPIENT_ID.toString())))).
		thenReturn(Collections.singletonList(ug));
		
		mtu = new MessageToUser();
		mtu.setId(MESSAGE_ID);
		mtu.setCreatedBy(CREATOR_ID.toString());
		mtu.setRecipients(Collections.singleton(RECIPIENT_ID.toString()));
		mtu.setSubject("subject");
		mtu.setFileHandleId(FILE_HANDLE_ID);
		mtu.setNotificationUnsubscribeEndpoint(UNSUBSCRIBE_ENDPOINT);
		mtu.setUserProfileSettingEndpoint(PROFILE_SETTING_ENDPOINT);
		mtu.setIsNotificationMessage(false);
		mtu.setWithUnsubscribeLink(false);
		mtu.setWithProfileSettingLink(true);
		mtu.setTo("TO <to@foo.com>");
		mtu.setCc("CC <cc@foo.com>");
		mtu.setBcc("BCC <bcc@foo.com>");
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);

		when(messageDAO.createMessage(mtu)).thenReturn(mtu);
		fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);

	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateMessageWithoutUser() {
		messageManager.createMessage(null, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateMessageWithoutMessage() {
		messageManager.createMessage(creatorUserInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateMessageWithoutRecipients() {
		messageManager.createMessage(creatorUserInfo, new MessageToUser());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateMessageWithEmptyRecipients() {
		MessageToUser messageToUser = new MessageToUser();
		messageToUser.setRecipients(new HashSet<String>());
		messageManager.createMessage(creatorUserInfo, messageToUser);
	}
	
	@Test
	public void testCreateMessagePLAIN() throws Exception {
		fileHandle.setContentType("text/plain");
		String messageBody = "message body";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.createMessage(creatorUserInfo, mtu);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("Foo FOO <foo@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = MessageTestUtil.getBodyFromRawMessage(ser, "text/html");
		assertTrue(body.indexOf(messageBody)>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		verify(messageDAO, never()).canCreateMessage(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testCreateNotificationMessagePLAIN() throws Exception {
		mtu.setIsNotificationMessage(true);
		String from = EmailUtils.DEFAULT_EMAIL_ADDRESS_LOCAL_PART+StackConfigurationSingleton.singleton().getNotificationEmailSuffix();
		fileHandle.setContentType("text/plain");
		String messageBody = "message body";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.createMessage(creatorUserInfo, mtu);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertFalse(ser.getSource().equals("Foo FOO <foo@synapse.org>"));
		assertEquals(from, ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = MessageTestUtil.getBodyFromRawMessage(ser, "text/html");
		assertTrue(body.indexOf(messageBody)>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		verify(messageDAO, never()).canCreateMessage(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testCreateMessageHTML() throws Exception {
		fileHandle.setContentType("text/html");
		String messageBody = "<div>message body</div>";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.createMessage(creatorUserInfo, mtu);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("Foo FOO <foo@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = MessageTestUtil.getBodyFromRawMessage(ser, "text/html");
		assertTrue(body.indexOf(messageBody)>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		verify(messageDAO, never()).canCreateMessage(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testCreateMessageJSON() throws Exception {
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.createMessage(creatorUserInfo, mtu);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("Foo FOO <foo@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = new String(ser.getRawMessage().getData().array());
		assertTrue(body.indexOf("message body")>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		verify(messageDAO, never()).canCreateMessage(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testCreateMessageWithThrottlePassed() throws Exception {
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.createMessageWithThrottle(creatorUserInfo, mtu);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("Foo FOO <foo@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = new String(ser.getRawMessage().getData().array());
		assertTrue(body.indexOf("message body")>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		assertTrue(mtu.getWithProfileSettingLink());
		assertFalse(mtu.getIsNotificationMessage());
		assertFalse(mtu.getWithUnsubscribeLink());
		verify(messageDAO).canCreateMessage(anyString(), anyLong(), anyLong());
	}

	@Test
	public void testCreateMessageWithThrottleFailed() throws Exception {
		when(messageDAO.canCreateMessage(eq(CREATOR_ID.toString()), 
				anyLong(), anyLong())).thenReturn(false);
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		try {
			messageManager.createMessageWithThrottle(creatorUserInfo, mtu);
			fail("TooManyRequestException is expected");
		} catch (TooManyRequestsException e) {
			ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
			verify(sesClient, never()).sendRawEmail(argument.capture());
			verify(messageDAO).canCreateMessage(anyString(), anyLong(), anyLong());
		}
	}

	@Test
	public void testWelcomeEmail() throws Exception{
		messageManager.sendWelcomeEmail(RECIPIENT_ID, UNSUBSCRIBE_ENDPOINT);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(ser.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertTrue(body.indexOf("Welcome to Synapse!")>=0);
	}

	@Test
	public void testSendNewPasswordResetEmail() throws Exception{
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));

		String synapsePrefix = "https://synapse.org/";

		messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientUsernameAlias);
		
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(ser.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertTrue(body.contains("Please follow the link below to set your password."));
	}
	
	@Test
	public void testSendNewPasswordResetEmailWithEmailAlias() throws Exception {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));

		String synapsePrefix = "https://synapse.org/";

		messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientEmailAlias);

		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL_ALIAS, ser.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(ser.getRawMessage().getData().array()));
		String body = (String) ((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertTrue(body.contains("Please follow the link below to set your password."));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSendNewPassowrdResetEmailWithMismatchedEmailAlias() throws Exception {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));

		String synapsePrefix = "https://synapse.org/";

		PrincipalAlias recipientEmailAlias = new PrincipalAlias();
		recipientEmailAlias.setAlias(RECIPIENT_EMAIL_ALIAS);
		recipientEmailAlias.setPrincipalId(RECIPIENT_ID + 1);
		recipientEmailAlias.setType(AliasType.USER_EMAIL);

		messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientEmailAlias);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testSendNewPasswordResetEmail_DisallowedSnapsePrefix() throws Exception{
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));
		String synapsePrefix = "https://NOTsynapse.org/";

		messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientUsernameAlias);
	}
	
	@Test
	public void testGetEmailForUser() {
		String email = messageManager.getEmailForUser(CREATOR_ID);
		verify(notificationEmailDao).getNotificationEmailForPrincipal(CREATOR_ID);
		assertEquals(CREATOR_EMAIL, email);
	}
	
	@Test
	public void testGetEmailForAliasWithUserNameAlias() {
		String email = messageManager.getEmailForAlias(recipientUsernameAlias);
		verify(notificationEmailDao).getNotificationEmailForPrincipal(recipientUsernameAlias.getPrincipalId());
		assertEquals(RECIPIENT_EMAIL, email);
	}
	
	@Test
	public void testGetEmailForAliasWithEmailAlias() {
		String email = messageManager.getEmailForAlias(recipientEmailAlias);
		verify(notificationEmailDao, never()).getNotificationEmailForPrincipal(anyLong());
		assertEquals(RECIPIENT_EMAIL_ALIAS, email);
	}
	
	@Test
	public void testGetPasswordResetUrl() {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String synapsePrefix = "https://synapse.org/";
		String expectedUrl = synapsePrefix + serializedToken;
		String resetUrl = messageManager.getPasswordResetUrl(synapsePrefix, token);
		assertEquals(expectedUrl, resetUrl);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetPasswordResetUrlWithMalformedUrl() {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));
		String synapsePrefix = "https://malformedSynapse.org/";
		messageManager.getPasswordResetUrl(synapsePrefix, token);
	}
	
	@Test
	public void testSendPasswordChangeConfirmationEmail() throws Exception{
		messageManager.sendPasswordChangeConfirmationEmail(RECIPIENT_ID);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(ser.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertTrue(body.contains("Your password for your Synapse account has been changed."));
	}


	@Test
	public void testSendDeliveryFailureEmail() throws Exception {
		List<String> errors = new ArrayList<String>();
		messageManager.sendDeliveryFailureEmail(MESSAGE_ID, errors);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(CREATOR_EMAIL, ser.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(ser.getRawMessage().getData().array()));
		String body = (String)((MimeMultipart) mimeMessage.getContent()).getBodyPart(0).getContent();
		assertTrue(body.indexOf("The following errors were experienced while delivering message")>=0);
		assertTrue(body.indexOf(mtu.getSubject())>=0);
	}
	
	@Test
	public void testSendMessageTo_AUTH_USERS() throws Exception {
		Long authUsersId = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		UserGroup ug = new UserGroup();
		ug.setId(authUsersId.toString());
		ug.setIsIndividual(false);
		when(userGroupDAO.get(eq(authUsersId))).thenReturn(ug);

		fileHandle.setContentType("text/plain");
		String messageBody = "message body";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);

		when(authorizationManager.canAccess(creatorUserInfo, authUsersId.toString(),
				ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationStatus.accessDenied(""));
		
		// This will fail since non-admin users do not have permission to send to the public group
		mtu.setRecipients(Collections.singleton(authUsersId.toString()));
		List<String> errors = messageManager.processMessage(MESSAGE_ID, null);
		String joinedErrors = StringUtils.join(errors, "\n");
		assertTrue(joinedErrors.contains("may not send"));
		
		// But an admin can do it
		UserInfo adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(CREATOR_ID);
		adminUserInfo.setGroups(Collections.singleton(CREATOR_ID));
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(adminUserInfo);
		
		when(authorizationManager.canAccess(creatorUserInfo, authUsersId.toString(),
				ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationStatus.authorized());
		
		errors = messageManager.processMessage(MESSAGE_ID, null);
		assertEquals(StringUtils.join(errors, "\n"), 0, errors.size());
	}

	@Test
	public void testForwardMessage() throws Exception {
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		MessageToUser dto = messageManager.createMessageWithThrottle(creatorUserInfo, mtu);

		reset(sesClient);
		MessageRecipientSet receipients = new MessageRecipientSet();
		receipients.setRecipients(dto.getRecipients());
		messageManager.forwardMessage(creatorUserInfo, dto.getId(), receipients);
		ArgumentCaptor<SendRawEmailRequest> argument = ArgumentCaptor.forClass(SendRawEmailRequest.class);

		verify(sesClient).sendRawEmail(argument.capture());
		SendRawEmailRequest ser = argument.getValue();
		assertEquals("Foo FOO <foo@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestinations().size());
		assertEquals(RECIPIENT_EMAIL, ser.getDestinations().get(0));
		String body = new String(ser.getRawMessage().getData().array());
		assertTrue(body.indexOf("message body")>=0);
		assertFalse(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertTrue(body.indexOf(PROFILE_SETTING_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
		assertTrue(mtu.getWithProfileSettingLink());
		assertFalse(mtu.getIsNotificationMessage());
		assertFalse(mtu.getWithUnsubscribeLink());
	}
}
