package org.sagebionetworks.repo.manager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.PasswordResetSignedToken;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.util.MessageTestUtil;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.SerializationUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class MessageManagerImplUnitTest {
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
	private EntityAclManager entityAclManager;
	@Mock
	private SynapseEmailService sesClient;
	@Mock
	private FileHandleManager fileHandleManager;
	@Mock
	private EmailQuarantineDao mockEmailQuarantineDao;
	
	@InjectMocks
	private MessageManagerImpl messageManager;
	
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
	private UserProfile userProfileCreator = null;
	private UserProfile userProfileRecipient = null;
	
	@BeforeEach
	public void setUp() throws Exception {
		
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
		
		userProfileCreator = new UserProfile();
		userProfileCreator.setFirstName("Foo");
		userProfileCreator.setLastName("FOO");
		userProfileCreator.setOwnerId(CREATOR_ID.toString());
		userProfileCreator.setUserName("foo");
		
		userProfileRecipient = new UserProfile();
		userProfileRecipient.setFirstName("Bar");
		userProfileRecipient.setLastName("BAR");
		userProfileRecipient.setOwnerId(RECIPIENT_ID.toString());
		userProfileRecipient.setUserName("bar");
		
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
		
		fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);

	}

	private void setupCreatorRecipientMocks(boolean overrideNotificationSettings) {
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(creatorUserInfo);
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		when(userProfileManager.getUserProfile(CREATOR_ID.toString())).thenReturn(userProfileCreator);
		
		// When overriding the notification settings the profile of the recipient is never fetched
		if (!overrideNotificationSettings) {
			when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfileRecipient);
		}
		
		UserGroup ug = new UserGroup();
		ug.setId(RECIPIENT_ID.toString());
		ug.setIsIndividual(true);
		
		when(userGroupDAO.get(eq(RECIPIENT_ID))).thenReturn(ug);
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		when(messageDAO.overrideNotificationSettings(MESSAGE_ID)).thenReturn(overrideNotificationSettings);
	}

	@Test
	public void testCreateMessageWithoutUser() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			messageManager.createMessage(null, null);
		});
	}

	@Test
	public void testCreateMessageWithoutMessage() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			messageManager.createMessage(creatorUserInfo, null);
		});
	}

	@Test
	public void testCreateMessageWithoutRecipients() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			messageManager.createMessage(creatorUserInfo, new MessageToUser());
		});
	}

	@Test
	public void testCreateMessageWithEmptyRecipients() {
		MessageToUser messageToUser = new MessageToUser();
		messageToUser.setRecipients(new HashSet<String>());
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			messageManager.createMessage(creatorUserInfo, messageToUser);
		});		
	}
	
	@Test
	public void testCreateMessage() {
		when(authorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		UserGroup ug = new UserGroup();
		ug.setId("1");
		ug.setIsIndividual(true);
		
		when(userGroupDAO.get(anyList())).thenReturn(Arrays.asList(ug));
		
		MessageToUser messageToUser = new MessageToUser();
		
		messageToUser.setFileHandleId(FILE_HANDLE_ID);
		messageToUser.setRecipients(Collections.singleton("1"));
		
		// Call under test
		messageManager.createMessage(creatorUserInfo, messageToUser);
		
		verify(authorizationManager).canAccessRawFileHandleById(creatorUserInfo, FILE_HANDLE_ID);
		verify(userGroupDAO).get(new ArrayList<>(messageToUser.getRecipients()));
	
		ArgumentCaptor<MessageToUser> captor = ArgumentCaptor.forClass(MessageToUser.class);
		
		verify(messageDAO).createMessage(captor.capture(), eq(false));
	}
	
	@Test
	public void testCreateMessageWithOverrideNotificationsettings() {
		when(authorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		UserGroup ug = new UserGroup();
		ug.setId("1");
		ug.setIsIndividual(true);
		
		when(userGroupDAO.get(anyList())).thenReturn(Arrays.asList(ug));
		
		MessageToUser messageToUser = new MessageToUser();
		
		messageToUser.setFileHandleId(FILE_HANDLE_ID);
		messageToUser.setRecipients(Collections.singleton("1"));
		
		// Call under test
		messageManager.createMessage(creatorUserInfo, messageToUser, true);
		
		verify(authorizationManager).canAccessRawFileHandleById(creatorUserInfo, FILE_HANDLE_ID);
		verify(userGroupDAO).get(new ArrayList<>(messageToUser.getRecipients()));
	
		ArgumentCaptor<MessageToUser> captor = ArgumentCaptor.forClass(MessageToUser.class);
		
		verify(messageDAO).createMessage(captor.capture(), eq(true));
	}
	
	@Test
	public void testProcessMessagePLAIN() throws Exception {
		setupCreatorRecipientMocks(false);
		
		fileHandle.setContentType("text/plain");
		String messageBody = "message body";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.processMessage(MESSAGE_ID, null);
		
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
	public void testProcessMessageHTML() throws Exception {
		setupCreatorRecipientMocks(false);
		
		fileHandle.setContentType("text/html");
		String messageBody = "<div>message body</div>";
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.processMessage(MESSAGE_ID, null);
		
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
	public void testProcessMessageJSON() throws Exception {
		setupCreatorRecipientMocks(false);
		
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		messageManager.processMessage(MESSAGE_ID, null);
		
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
	public void testCreateMessageWithThrottleFailed() throws Exception {
		when(messageDAO.canCreateMessage(eq(CREATOR_ID.toString()), anyLong(), anyLong())).thenReturn(false);
		
		Assertions.assertThrows(TooManyRequestsException.class, ()->{
			messageManager.createMessageWithThrottle(creatorUserInfo, mtu);
		});
		
		verify(messageDAO).canCreateMessage(anyString(), anyLong(), anyLong());
		verifyNoMoreInteractions(messageDAO);
	}

	@Test
	public void testSendNewPasswordResetEmail() throws Exception{
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);		
		when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfileRecipient);
		
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
	public void testSendNewPasswordResetEmailWithQuarantinedAddress() throws Exception {
		
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		when(mockEmailQuarantineDao.isQuarantined(RECIPIENT_EMAIL)).thenReturn(true);
		
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));

		String synapsePrefix = "https://synapse.org/";

		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {
			messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientUsernameAlias);
		});
		
		verify(mockEmailQuarantineDao).isQuarantined(RECIPIENT_EMAIL);
		verifyZeroInteractions(sesClient);
	}
	
	@Test
	public void testSendNewPasswordResetEmailWithEmailAlias() throws Exception {
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfileRecipient);
		
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

	@Test
	public void testSendNewPassowrdResetEmailWithMismatchedEmailAlias() throws Exception {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));

		String synapsePrefix = "https://synapse.org/";

		PrincipalAlias recipientEmailAlias = new PrincipalAlias();
		recipientEmailAlias.setAlias(RECIPIENT_EMAIL_ALIAS);
		recipientEmailAlias.setPrincipalId(RECIPIENT_ID + 1);
		recipientEmailAlias.setType(AliasType.USER_EMAIL);

		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientEmailAlias);
		});
	}


	@Test
	public void testSendNewPasswordResetEmail_DisallowedSnapsePrefix() throws Exception{
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));
		String synapsePrefix = "https://NOTsynapse.org/";
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			messageManager.sendNewPasswordResetEmail(synapsePrefix, token, recipientUsernameAlias);
		});
	}
	
	@Test
	public void testGetEmailForUser() {
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn(CREATOR_EMAIL);
		
		String email = messageManager.getEmailForUser(CREATOR_ID);
		verify(notificationEmailDao).getNotificationEmailForPrincipal(CREATOR_ID);
		assertEquals(CREATOR_EMAIL, email);
	}
	
	@Test
	public void testGetEmailForAliasWithUserNameAlias() {
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		
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
	
	@Test
	public void testGetPasswordResetUrlWithMalformedUrl() {
		PasswordResetSignedToken token = new PasswordResetSignedToken();
		token.setUserId(Long.toString(RECIPIENT_ID));
		String synapsePrefix = "https://malformedSynapse.org/";
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {			
			messageManager.getPasswordResetUrl(synapsePrefix, token);
		});
	}
	
	@Test
	public void testSendPasswordChangeConfirmationEmail() throws Exception{
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfileRecipient);
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		
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
	public void testSendPasswordChangeConfirmationEmailWithQuarantinedAddress() throws Exception {
		when(principalAliasDAO.getUserName(RECIPIENT_ID)).thenReturn("bar");
		when(userProfileManager.getUserProfile(RECIPIENT_ID.toString())).thenReturn(userProfileRecipient);
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn(RECIPIENT_EMAIL);
		when(mockEmailQuarantineDao.isQuarantined(RECIPIENT_EMAIL)).thenReturn(true);
		
		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {
			messageManager.sendPasswordChangeConfirmationEmail(RECIPIENT_ID);
		});
		
		verify(mockEmailQuarantineDao).isQuarantined(RECIPIENT_EMAIL);
		verifyZeroInteractions(sesClient);

	}


	@Test
	public void testSendDeliveryFailureEmail() throws Exception {
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn(CREATOR_EMAIL);
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		
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
	public void testSendDeliveryFailureEmailToNotificationMessage() throws Exception {
		
		mtu.setIsNotificationMessage(true);
		
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		
		List<String> errors = new ArrayList<String>();
		
		// Call under test
		messageManager.sendDeliveryFailureEmail(MESSAGE_ID, errors);
		
		verifyZeroInteractions(sesClient);
	}
	
	@Test
	public void testSendDeliveryFailureEmailWithQuarantinedAddress() throws Exception {
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn(CREATOR_EMAIL);
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		
		when(mockEmailQuarantineDao.isQuarantined(CREATOR_EMAIL)).thenReturn(true);
		
		List<String> errors = new ArrayList<String>();
		
		Assertions.assertThrows(QuarantinedEmailException.class, ()-> {
			messageManager.sendDeliveryFailureEmail(MESSAGE_ID, errors);
		});
		
		verify(mockEmailQuarantineDao).isQuarantined(CREATOR_EMAIL);
		verifyZeroInteractions(sesClient);
	}
	
	@Test
	public void testSendMessageTo_AUTH_USERS() throws Exception {
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(creatorUserInfo);
		when(userProfileManager.getUserProfile(CREATOR_ID.toString())).thenReturn(userProfileCreator);
		when(principalAliasDAO.getUserName(CREATOR_ID)).thenReturn("foo");
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		
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
		
		when(authorizationManager.canAccess(adminUserInfo, authUsersId.toString(),
				ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationStatus.authorized());
		
		errors = messageManager.processMessage(MESSAGE_ID, null);
		assertEquals(StringUtils.join(errors, "\n"), 0, errors.size());
	}
	
	@Test
	public void testSendMessageWithQuarantinedAddress() throws Exception {
		setupCreatorRecipientMocks(false);
		fileHandle.setContentType("text/plain");
		
		String messageBody = "message body";
		
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn(messageBody);
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		when(mockEmailQuarantineDao.isQuarantined(RECIPIENT_EMAIL)).thenReturn(true);
		
		List<String> errors = messageManager.processMessage(MESSAGE_ID, null);
		
		verify(mockEmailQuarantineDao).isQuarantined(RECIPIENT_EMAIL);
		assertEquals(ImmutableList.of("Cannot deliver message to recipient (" + RECIPIENT_ID + "). The recipient does not have a valid notification email."), errors);
		verifyZeroInteractions(sesClient);
	}

	@Test
	public void testForwardMessage() throws Exception {
		when(messageDAO.canCreateMessage(eq(CREATOR_ID.toString()), anyLong(), anyLong())).thenReturn(true);
		when(authorizationManager.canAccessRawFileHandleById(creatorUserInfo, FILE_HANDLE_ID)).thenReturn(AuthorizationStatus.authorized());
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);
		when(messageDAO.createMessage(any(), anyBoolean())).thenReturn(mtu);
		
		UserGroup ug = new UserGroup();
		ug.setId(RECIPIENT_ID.toString());
		ug.setIsIndividual(true);
		
		when(userGroupDAO.get(eq(Collections.singletonList(RECIPIENT_ID.toString())))).thenReturn(Collections.singletonList(ug));
		
		MessageRecipientSet recipients = new MessageRecipientSet();
		recipients.setRecipients(Collections.singleton(RECIPIENT_ID.toString()));
		
		MessageToUser forwardedDto = messageManager.forwardMessage(creatorUserInfo, MESSAGE_ID, recipients);
		
		verify(messageDAO).getMessage(MESSAGE_ID);
		verify(messageDAO).createMessage(mtu, false);
		
		assertEquals(mtu, forwardedDto);
		
		
	}
	
	@Test
	public void testProcessMessage() throws Exception {
		setupCreatorRecipientMocks(false);
		
		fileHandle.setContentType("application/json");
		MessageBody messageBody = new MessageBody();
		messageBody.setPlain("message body");
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).
		thenReturn(EntityFactory.createJSONStringForEntity(messageBody));
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		// Call under test
		messageManager.processMessage(MESSAGE_ID, null);
		
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
	
	@Test
	public void testProcessMessageWithOverrideNotificationSettings() throws Exception {		
		setupCreatorRecipientMocks(true);
		
		fileHandle.setContentType("application/json");
		
		when(fileHandleManager.downloadFileToString(FILE_HANDLE_ID)).thenReturn("message body");
		when(fileHandleDAO.get(FILE_HANDLE_ID)).thenReturn(fileHandle);
		
		// Call under test
		messageManager.processMessage(MESSAGE_ID, null);
		
		// Verify that no call to the recipient user profile is performed
		verify(userProfileManager, times(0)).getUserProfile(RECIPIENT_ID.toString());
		
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
