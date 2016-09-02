package org.sagebionetworks.repo.manager;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.util.MessageTestUtil;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;


public class MessageManagerImplUnitTest {
	private MessageManagerImpl messageManager;
	private UserManager userManager;
	private MessageDAO messageDAO;
	private UserGroupDAO userGroupDAO;
	private GroupMembersDAO groupMembersDao;
	private UserProfileManager userProfileManager;
	private NotificationEmailDAO notificationEmailDao;
	private PrincipalAliasDAO principalAliasDAO;
	private AuthorizationManager authorizationManager;
	private FileHandleDao fileHandleDAO;
	private NodeDAO nodeDAO;
	private EntityPermissionsManager entityPermissionsManager;
	private SynapseEmailService sesClient;
	private FileHandleManager fileHandleManager;
	
	private MessageToUser mtu;
	private FileHandle fileHandle;
	
	private static final String MESSAGE_ID = "101";
	private static final Long CREATOR_ID = 999L;
	private static final Long RECIPIENT_ID = 888L;
	private static final String FILE_HANDLE_ID = "222";
	private UserInfo creatorUserInfo = null;
	private static final String UNSUBSCRIBE_ENDPOINT = "https://www.synapse.org/#unsub:";
	
	@Before
	public void setUp() throws Exception {
		messageDAO = Mockito.mock(MessageDAO.class);
		userGroupDAO = Mockito.mock(UserGroupDAO.class);
		groupMembersDao = Mockito.mock(GroupMembersDAO.class);
		userManager = Mockito.mock(UserManager.class);
		userProfileManager = Mockito.mock(UserProfileManager.class);
		notificationEmailDao = Mockito.mock(NotificationEmailDAO.class);
		principalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		authorizationManager = Mockito.mock(AuthorizationManager.class);
		fileHandleDAO = Mockito.mock(FileHandleDao.class);
		nodeDAO = Mockito.mock(NodeDAO.class);
		entityPermissionsManager = Mockito.mock(EntityPermissionsManager.class);
		sesClient = Mockito.mock(SynapseEmailService.class);
		fileHandleManager = Mockito.mock(FileHandleManager.class);
		
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
		when(userManager.getUserInfo(CREATOR_ID)).thenReturn(creatorUserInfo);
		
		when(notificationEmailDao.getNotificationEmailForPrincipal(CREATOR_ID)).thenReturn("foo@sagebase.org");
		when(notificationEmailDao.getNotificationEmailForPrincipal(RECIPIENT_ID)).thenReturn("bar@sagebase.org");
		
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
			thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
		mtu.setTo("TO <to@foo.com>");
		mtu.setCc("CC <cc@foo.com>");
		mtu.setBcc("BCC <bcc@foo.com>");
		when(messageDAO.getMessage(MESSAGE_ID)).thenReturn(mtu);

		when(messageDAO.createMessage(mtu)).thenReturn(mtu);
		fileHandle = new S3FileHandle();
		fileHandle.setId(FILE_HANDLE_ID);

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
		assertEquals("bar@sagebase.org", ser.getDestinations().get(0));
		String body = MessageTestUtil.getBodyFromRawMessage(ser, "text/html");
		assertTrue(body.indexOf(messageBody)>=0);
		assertTrue(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
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
		assertEquals("bar@sagebase.org", ser.getDestinations().get(0));
		String body = MessageTestUtil.getBodyFromRawMessage(ser, "text/html");
		assertTrue(body.indexOf(messageBody)>=0);
		assertTrue(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
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
		assertEquals("bar@sagebase.org", ser.getDestinations().get(0));
		String body = new String(ser.getRawMessage().getData().array());
		assertTrue(body.indexOf("message body")>=0);
		assertTrue(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
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
		assertEquals("bar@sagebase.org", ser.getDestinations().get(0));
		String body = new String(ser.getRawMessage().getData().array());
		assertTrue(body.indexOf("message body")>=0);
		assertTrue(body.indexOf(UNSUBSCRIBE_ENDPOINT)>=0);
		assertEquals(mtu.getSubject(), MessageTestUtil.getSubjectFromRawMessage(ser));
		assertEquals(mtu.getTo(), MessageTestUtil.getHeaderFromRawMessage(ser, "To"));
		assertEquals(mtu.getCc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Cc"));
		assertEquals(mtu.getBcc(), MessageTestUtil.getHeaderFromRawMessage(ser, "Bcc"));
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
	public void testWelcomeEmail() {
		messageManager.sendWelcomeEmail(RECIPIENT_ID, DomainType.SYNAPSE, UNSUBSCRIBE_ENDPOINT);
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesClient).sendEmail(argument.capture());
		SendEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestination().getToAddresses().size());
		assertEquals("bar@sagebase.org", ser.getDestination().getToAddresses().get(0));
		assertTrue(ser.getMessage().getBody().getText().getData().indexOf(
				"Welcome to Synapse!")>=0);
	}

	@Test
	public void testPasswordResetEmail() {
		messageManager.sendPasswordResetEmail(RECIPIENT_ID, DomainType.SYNAPSE, "abcdefg");
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesClient).sendEmail(argument.capture());
		SendEmailRequest ser = argument.getValue();
		assertEquals("Bar BAR <bar@synapse.org>", ser.getSource());
		assertEquals(1, ser.getDestination().getToAddresses().size());
		assertEquals("bar@sagebase.org", ser.getDestination().getToAddresses().get(0));
		assertTrue(ser.getMessage().getBody().getText().getData().indexOf(
				"Please follow the link below to set your password.")>=0);
	}


	@Test
	public void testSendDeliveryFailureEmail() {
		List<String> errors = new ArrayList<String>();
		messageManager.sendDeliveryFailureEmail(MESSAGE_ID, errors);
		ArgumentCaptor<SendEmailRequest> argument = ArgumentCaptor.forClass(SendEmailRequest.class);
		verify(sesClient).sendEmail(argument.capture());
		SendEmailRequest ser = argument.getValue();
		assertEquals("noreply@synapse.org", ser.getSource());
		assertEquals(1, ser.getDestination().getToAddresses().size());
		assertEquals("foo@sagebase.org", ser.getDestination().getToAddresses().get(0));
		assertTrue(ser.getMessage().getBody().getText().getData().indexOf(
				"The following errors were experienced while delivering message")>=0);
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
				ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);	
		
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
				ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);	
		
		errors = messageManager.processMessage(MESSAGE_ID, null);
		assertEquals(StringUtils.join(errors, "\n"), 0, errors.size());
	}

	
	
}
