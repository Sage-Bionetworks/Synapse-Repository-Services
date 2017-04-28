package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * Tests message access requirement checking and the sending of messages
 * Note: only the logic for sending messages is tested, a separate test handles tests of sending emails
 * 
 * Sorting of messages is not tested.  All tests order their results as most recent first.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageManagerImplTest {
	
	private MessageManagerImpl messageManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private MembershipRequestManager membershipRequestManager;
	
	@Autowired
	private FileHandleDao fileDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private FileHandleManager mockFileHandleManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private GroupMembersDAO groupMembersDao;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private AmazonSimpleEmailService amazonSESClient;
	
	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;
	
	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private IdGenerator idGenerator;
	

	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = true;
	private static final long LIMIT = 100;
	private static final long OFFSET = 0;
	
	// Mutual spammers
	private UserInfo testUser;
	private UserInfo otherTestUser;
	private UserInfo trustedMessageSender;
	private Team testTeam;
	
	private String fileHandleId;
	private String tmsFileHandleId;
	
	@SuppressWarnings("serial")
	private final List<MessageStatusType> unreadMessageFilter = new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}};
	
	private MessageToUser userToOther;
	private MessageToUser otherReplyToUser;
	private MessageToUser userReplyToOtherAndSelf;
	private MessageToUser otherReplyToUserAndSelf;
	private MessageToUser userToSelfAndGroup;
	private MessageToUser otherToGroup;
	
	private UserInfo adminUserInfo;
	private List<String> cleanup;
	private String nodeId;
	private String childId;
	
	private List<PrincipalAlias> aliasesToDelete;
	
	/**
	 * Note: This setup is very similar to {@link #DBOMessageDAOImplTest}
	 */
	@SuppressWarnings("serial")
	@Before
	public void setUp() throws Exception {
		messageManager = new MessageManagerImpl();
		ReflectionTestUtils.setField(messageManager, "messageDAO", messageDAO);
		ReflectionTestUtils.setField(messageManager, "userGroupDAO", userGroupDAO);
		ReflectionTestUtils.setField(messageManager, "groupMembersDAO", groupMembersDao);
		ReflectionTestUtils.setField(messageManager, "userManager", userManager);
		ReflectionTestUtils.setField(messageManager, "userProfileManager", userProfileManager);
		ReflectionTestUtils.setField(messageManager, "notificationEmailDao", notificationEmailDao);
		ReflectionTestUtils.setField(messageManager, "principalAliasDAO", principalAliasDAO);
		ReflectionTestUtils.setField(messageManager, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(messageManager, "fileHandleDao", fileDAO);
		ReflectionTestUtils.setField(messageManager, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(messageManager, "entityPermissionsManager", entityPermissionsManager);
		ReflectionTestUtils.setField(messageManager, "nodeInheritanceManager", nodeInheritanceManager);
		// the normally autowired SynapseEmailService diverts email from Amazon SES into an S3
		// file for testing.  In this test suite, however, we want mail to actually go to SES,
		// so we override the normal autowiring.
		ReflectionTestUtils.setField(messageManager, "sesClient", 
				new SynapseEmailService() {
					@Override
					public void sendEmail(SendEmailRequest emailRequest) {
						amazonSESClient.sendEmail(emailRequest);
					}

					@Override
					public void sendRawEmail(
							SendRawEmailRequest sendRawEmailRequest) {
						amazonSESClient.sendRawEmail(sendRawEmailRequest);
					}});
		mockFileHandleManager = mock(FileHandleManager.class);
		ReflectionTestUtils.setField(messageManager, "fileHandleManager", mockFileHandleManager);
		

		aliasesToDelete = new ArrayList<PrincipalAlias>();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		cleanup = new ArrayList<String>();
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		
		// Need two users for this test
		{
			NewUser nu = new NewUser();
			nu.setEmail(UUID.randomUUID().toString() + "@test.com");
			nu.setUserName(UUID.randomUUID().toString());
			testUser = userManager.createUser(adminUserInfo, nu, cred, tou);
			
			nu = new NewUser();
			nu.setEmail(UUID.randomUUID().toString() + "@test.com");
			nu.setUserName(UUID.randomUUID().toString());
			otherTestUser = userManager.createUser(adminUserInfo,nu, cred, tou);
			
			tou.setPrincipalId(otherTestUser.getId());
			basicDao.createOrUpdate(tou);
		}
		
		{
			NewUser nu2 = new NewUser();
			nu2.setEmail(UUID.randomUUID().toString() + "@test.com");
			nu2.setUserName(UUID.randomUUID().toString());
			trustedMessageSender = userManager.createUser(adminUserInfo, nu2, cred, tou);
			tou.setPrincipalId(trustedMessageSender.getId());
			basicDao.createOrUpdate(tou);
			// now add to trusted users group
			groupMembersDao.addMembers(
					""+TeamConstants.TRUSTED_MESSAGE_SENDER_TEAM_ID, 
					Collections.singletonList(trustedMessageSender.getId().toString()));
			// now we update the object with the new list of groups
			trustedMessageSender = userManager.getUserInfo(trustedMessageSender.getId());
			assertTrue(trustedMessageSender.getGroups().contains(TeamConstants.TRUSTED_MESSAGE_SENDER_TEAM_ID));
		}

		// Create a team
		testTeam = new Team();
		testTeam.setName("MessageManagerImplTest");
		testTeam = teamManager.create(testUser, testTeam);
		final String testTeamId = testTeam.getId();
		
		// we don't want the authenticated users to be able to send messages to the team
		AccessControlList acl = teamManager.getACL(testUser, testTeamId);
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (!ra.getPrincipalId().equals(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId())) {
				ras.add(ra);
			}
		}
		acl.setResourceAccess(ras);
		teamManager.updateACL(testUser, acl);
		
		// Mock out the file handle manager so that the fake file handle won't result in broken downloads
		String url = MessageManagerImplTest.class.getClassLoader().getResource("images/notAnImage.txt").toExternalForm();
		when(mockFileHandleManager.getRedirectURLForFileHandle(anyString())).thenReturn(url);
		ReflectionTestUtils.setField(messageManager, "fileHandleManager", mockFileHandleManager);
		
		// This user info needs to be updated to contain the team
		testUser = userManager.getUserInfo(testUser.getId());
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		final String testUserId = testUser.getId().toString();
		{
			S3FileHandle handle = TestUtils.createS3FileHandle(testUserId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
			handle = (S3FileHandle) fileDAO.createFile(handle);
			this.fileHandleId = handle.getId();
			when(mockFileHandleManager.createCompressedFileFromString(eq(testUserId), any(Date.class), anyString())).thenReturn(handle);
			when(mockFileHandleManager.downloadFileToString(fileHandleId)).thenReturn("some message body");
		}
		
		{
			String tmsUserId = trustedMessageSender.getId().toString();
			S3FileHandle handle = TestUtils.createS3FileHandle(tmsUserId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
			handle = (S3FileHandle) fileDAO.createFile(handle);
			this.tmsFileHandleId = handle.getId();
			when(mockFileHandleManager.
					createCompressedFileFromString(eq(tmsUserId), any(Date.class), anyString())).thenReturn(handle);
			when(mockFileHandleManager.downloadFileToString(tmsFileHandleId)).thenReturn("some other message body");
		}
		
		// Create all the messages
		// These will be sent automatically since they have only one recipient
		final String otherTestUserId = otherTestUser.getId().toString();
		userToOther = createMessage(testUser, "userToOther", 
				new HashSet<String>() {{add(otherTestUserId);}}, null);
		otherReplyToUser = createMessage(otherTestUser, "otherReplyToUser", 
				new HashSet<String>() {{add(testUserId);}}, userToOther.getId());
		
		// These must be sent by a worker
		userReplyToOtherAndSelf = createMessage(testUser, "userReplyToOtherAndSelf", 
				new HashSet<String>() {{add(testUserId); add(otherTestUserId);}}, otherReplyToUser.getId());
		otherReplyToUserAndSelf = createMessage(otherTestUser, "otherReplyToUserAndSelf", 
				new HashSet<String>() {{add(testUserId); add(otherTestUserId);}}, userReplyToOtherAndSelf.getId());
		userToSelfAndGroup = createMessage(testUser, "userToSelfAndGroup", 
				new HashSet<String>() {{add(testUserId); add(testTeamId);}}, null);
		otherToGroup = createMessage(otherTestUser, "otherToGroup", 
				new HashSet<String>() {{add(testTeamId);}}, null);
	}
	
	@After
	public void tearDown() throws Exception {
		for (String id : cleanup) {
			messageManager.deleteMessage(adminUserInfo, id);
		}
		
		fileDAO.delete(fileHandleId);

		// Cleanup the team
		teamManager.delete(testUser, testTeam.getId());
		
		if (nodeId != null) {
			try {
				nodeManager.delete(adminUserInfo, nodeId);
			} catch (NotFoundException e) { }
		}
		nodeId=null;
		
		if (childId != null) {
			try {
				nodeManager.delete(adminUserInfo, childId);
			} catch (NotFoundException e) { }
		}
		childId=null;
		
		// Reset the test user's notification settings to the default
		UserProfile profile = userProfileManager.getUserProfile(testUser.getId().toString());
		profile.setNotificationSettings(new Settings());
		userProfileManager.updateUserProfile(testUser, profile);
		
		userManager.deletePrincipal(adminUserInfo, testUser.getId());
		userManager.deletePrincipal(adminUserInfo, otherTestUser.getId());
		for (PrincipalAlias alias : aliasesToDelete) {
			principalAliasDAO.removeAliasFromPrincipal(alias.getPrincipalId(), alias.getAliasId());
		}
		if (testTeam!=null) {
			teamManager.delete(adminUserInfo, testTeam.getId());
			testTeam=null;
		}
	}
	
	/**
	 * Creates a message row
	 */
	private MessageToUser createMessageWithThrottle(UserInfo userInfo, String subject, String fileHandleId, Set<String> recipients, String inReplyTo) throws InterruptedException, NotFoundException {
		assertNotNull(userInfo);
		
		MessageToUser dto = new MessageToUser();
		// Note: ID is auto generated
		// Note: CreatedBy is derived from userInfo
		dto.setFileHandleId(fileHandleId);
		// Note: CreatedOn is set by the DAO
		dto.setSubject(subject);
		dto.setRecipients(recipients);
		dto.setInReplyTo(inReplyTo);
		dto.setNotificationUnsubscribeEndpoint("https://www.synapse.org/#unsubscribeEndpoint:");
		// Note: InReplyToRoot is calculated by the DAO
		
		// Insert the message
		dto = messageManager.createMessageWithThrottle(userInfo, dto);
		assertNotNull(dto.getId());
		cleanup.add(dto.getId());
		assertEquals(userInfo.getId().toString(), dto.getCreatedBy());
		assertNotNull(dto.getCreatedOn());
		assertNotNull(dto.getInReplyToRoot());

		// Make sure the timestamps on the messages are different 
		Thread.sleep(5L); // just 5 milliseconds
		
		return messageManager.getMessage(userInfo, dto.getId());
	}
	
	/**
	 * Creates a message row
	 */
	private MessageToUser createMessage(UserInfo userInfo, String subject, Set<String> recipients, String inReplyTo) throws InterruptedException, NotFoundException {
		return createMessageWithThrottle(userInfo, subject, fileHandleId, recipients, inReplyTo);
	}
		
	
	/**
	 * Sends the messages that must be sent by a worker
	 * 
	 * @param send_otherToSelfAndGroup This message may or may not have the proper permissions associated with it
	 */
	private List<String> sendUnsentMessages(boolean send_otherToGroup) throws Exception {
		assertEquals(0, messageManager.processMessage(userReplyToOtherAndSelf.getId(), null).size());
		assertEquals(0, messageManager.processMessage(otherReplyToUserAndSelf.getId(), null).size());
		assertEquals(0, messageManager.processMessage(userToSelfAndGroup.getId(), null).size());
		if (send_otherToGroup) {
			return messageManager.processMessage(otherToGroup.getId(), null);
		}
		return new ArrayList<String>();
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testSendMessageFromAnonymous() throws Exception {
		UserInfo anonymousUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		Set<String> recipients = new HashSet<String>();
		recipients.add(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString());
		createMessage(anonymousUserInfo, "anonymous message", recipients, null);
	}
	
	
	@Test
	public void testNoResend() throws Exception {
		// two recipients but neither receives the message. 
		// Need to test that if called a second time the message is NOT resent, since sent=true.
		
		// the message subject tells the stubbed client to create a failure
		final String testUserId = testUser.getId().toString();
		final String otherTestUserId = otherTestUser.getId().toString();
		MessageToUser aMessage = createMessage(testUser, StubAmazonSimpleEmailServiceClient.MESSAGE_SUBJECT_FOR_FAILURE, 
				new HashSet<String>() {{add(testUserId); add(otherTestUserId);}}, null);
		
		List<MessageBundle> inbox = null;
		inbox = messageManager.getInbox(testUser, Collections.singletonList(MessageStatusType.UNREAD), MessageSortBy.SEND_DATE, true, 100, 0);
		int initialTestUserInboxSize = 1;
		assertEquals(initialTestUserInboxSize, inbox.size());
		int initialOtherTestUserInboxSize = 1;
		inbox = messageManager.getInbox(otherTestUser, Collections.singletonList(MessageStatusType.UNREAD), MessageSortBy.SEND_DATE, true, 100, 0);
		assertEquals(initialOtherTestUserInboxSize, inbox.size());
		
		// now send the message
		List<String> errors = messageManager.processMessage(aMessage.getId(), null);
		
		// check that the stubbed client -- 2 failures for the two recipients
		assertEquals(2, errors.size());
		for (String message : errors) {
			assertTrue(message.indexOf(StubAmazonSimpleEmailServiceClient.TRANSMISSION_FAILURE)>=0);
		}
		
		// even though the message is not sent by email, it does appear in the in-box
		inbox = messageManager.getInbox(testUser, Collections.singletonList(MessageStatusType.UNREAD), MessageSortBy.SEND_DATE, true, 100, 0);
		assertEquals(initialTestUserInboxSize+1, inbox.size());
		inbox = messageManager.getInbox(otherTestUser, Collections.singletonList(MessageStatusType.UNREAD), MessageSortBy.SEND_DATE, true, 100, 0);
		assertEquals(initialOtherTestUserInboxSize+1, inbox.size());

		// now send a second time
		errors = messageManager.processMessage(aMessage.getId(), null);
		
		// check that the stubbed client was NOT called
		assertEquals(0, errors.size());
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testGetMessagePermissions() throws Exception {
		// User should be able to see both messages that have been delivered
		assertEquals(userToOther, messageManager.getMessage(testUser, userToOther.getId()));
		assertEquals(otherReplyToUser, messageManager.getMessage(testUser, otherReplyToUser.getId()));
		
		// User should be able to get a message directed at it, but that hasn't been sent yet
		assertEquals(otherReplyToUserAndSelf, messageManager.getMessage(testUser, otherReplyToUserAndSelf.getId()));
		
		// User should be able to see a message that cannot be sent, but is directed at a group the user is in
		assertEquals(otherToGroup, messageManager.getMessage(testUser, otherToGroup.getId()));
		
		// User should not be able to see a message that the other user sends to itself
		final String otherId = otherTestUser.getId().toString();
		Set<String> otherIdSet = new HashSet<String>() {{add(otherId);}};
		MessageToUser invisible = createMessage(otherTestUser, "This is a personal reminder", otherIdSet, null);
		try {
			messageManager.getMessage(testUser, invisible.getId());
			fail();
		} catch (UnauthorizedException e) {
			assertTrue(e.getMessage().contains("not the sender or receiver"));
		}
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testForwardMessage() throws Exception {
		// Forward a message
		final String testUserId = testUser.getId().toString();
		MessageRecipientSet recipients = new MessageRecipientSet();
		recipients.setRecipients(new HashSet<String>() {{add(testUserId);}});
		MessageToUser forwarded = messageManager.forwardMessage(testUser, userToOther.getId(), recipients);
		cleanup.add(forwarded.getId());
		
		// It should appear in the test user's inbox immediately
		List<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(2, messages.size());
		assertEquals(forwarded, messages.get(0).getMessage());
		assertEquals(otherReplyToUser, messages.get(1).getMessage());
	}
	
	@Test
	public void testGetConversation_BeforeSending() throws Exception {
		List<MessageToUser> messages = messageManager.getConversation(testUser, userToOther.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3, messages.size());
		assertEquals(userReplyToOtherAndSelf, messages.get(0));
		assertEquals(otherReplyToUser, messages.get(1));
		assertEquals(userToOther, messages.get(2));
		
		messages = messageManager.getConversation(otherTestUser, otherReplyToUser.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3, messages.size());
		assertEquals(otherReplyToUserAndSelf, messages.get(0));
		assertEquals(otherReplyToUser, messages.get(1));
		assertEquals(userToOther, messages.get(2));
	}
	
	@Test
	public void testGetConversation_AfterSending() throws Exception {
		sendUnsentMessages(false);
		
		List<MessageToUser> messages = messageManager.getConversation(testUser, userToOther.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(4, messages.size());
		assertEquals(otherReplyToUserAndSelf, messages.get(0));
		assertEquals(userReplyToOtherAndSelf, messages.get(1));
		assertEquals(otherReplyToUser, messages.get(2));
		assertEquals(userToOther, messages.get(3));
		
		List<MessageToUser> whatTheOtherUserSees = messageManager.getConversation(otherTestUser, otherReplyToUser.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("The users have the same privileges regarding the thread's visibility", messages, whatTheOtherUserSees);
	}
	
	@Test
	public void testGetInbox_BeforeSending() throws Exception {
		List<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1, messages.size());
		assertEquals(otherReplyToUser, messages.get(0).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1, messages.size());
		assertEquals(userToOther, messages.get(0).getMessage());
	}
	
	@Test
	public void testGetInbox_AfterSending() throws Exception {
		sendUnsentMessages(false);
		
		List<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(4, messages.size());
		assertEquals(userToSelfAndGroup, messages.get(0).getMessage());
		assertEquals(otherReplyToUserAndSelf, messages.get(1).getMessage());
		assertEquals(userReplyToOtherAndSelf, messages.get(2).getMessage());
		assertEquals(otherReplyToUser, messages.get(3).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3, messages.size());
		assertEquals(otherReplyToUserAndSelf, messages.get(0).getMessage());
		assertEquals(userReplyToOtherAndSelf, messages.get(1).getMessage());
		assertEquals(userToOther, messages.get(2).getMessage());
	}
	
	@Test
	public void testGetOutbox() throws Exception {
		List<MessageToUser> messages = messageManager.getOutbox(testUser, 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3, messages.size());
		assertEquals(userToSelfAndGroup, messages.get(0));
		assertEquals(userReplyToOtherAndSelf, messages.get(1));
		assertEquals(userToOther, messages.get(2));
		
		sendUnsentMessages(false);
		
		List<MessageToUser> afterSending = messageManager.getOutbox(testUser, 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(messages, afterSending);
	}
	
	@Test
	public void testSendMessage_DoesntFailOnResend() throws Exception {
		List<MessageBundle> messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1, messages.size());
		
		messageManager.processMessage(userToOther.getId(), null);
		messageManager.processMessage(userToOther.getId(), null);
		messageManager.processMessage(userToOther.getId(), null);
		
		// Multiple calls to sendMessage do nothing
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1, messages.size());
	}
	
	@Test
	public void testSendMessage_NotAllowed() throws Exception {
		List<String> errors = sendUnsentMessages(true);
		assertEquals(1, errors.size());
		assertTrue(errors.get(0).contains("may not send"));
	}
	
	@Test
	public void testTrustedUserCanSendToAnyTeam() throws Exception {
		// Cannot send a message to a team you're not in...
		MessageToUser messageToTeam = createMessage(otherTestUser, "messageToTeam", 
					new HashSet<String>() {{add(testTeam.getId());}}, null);
		cleanup.add(messageToTeam.getId());
		assertEquals(1, messageManager.processMessage(messageToTeam.getId(), null).size());

		// ... unless you're a Trusted Message Sender
		messageToTeam = createMessageWithThrottle(trustedMessageSender, "messageToTeam", tmsFileHandleId,
				new HashSet<String>() {{add(testTeam.getId());}}, null);
		assertEquals(0, messageManager.processMessage(messageToTeam.getId(), null).size());
	}
	
	/**
	 * Bottom part of the test is related to {@link #testGetInbox_AfterSending()}
	 */
	@Test
	public void testSendMessage_AfterJoinTeam() throws Exception {
		// Join the team
		MembershipRqstSubmission request = new MembershipRqstSubmission();
		request.setUserId(otherTestUser.getId().toString());
		request.setTeamId(testTeam.getId());
		membershipRequestManager.create(otherTestUser, request);
		teamManager.addMember(testUser, testTeam.getId(), otherTestUser);
		
		// Now all the messages should be sent without error
		List<String> errors = sendUnsentMessages(true);
		assertEquals(0, errors.size());
		
		// The last message should show up in the testUser's inbox, even though the testUser was not in the recipient list
		List<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(otherToGroup, messages.get(0).getMessage());
	}
	
	@Test
	public void testCreateMessage_TooManyRecipients() throws Exception {
		Set<String> tooMany = new HashSet<String>();
		for (int i = 0; i < MessageManagerImpl.MAX_NUMBER_OF_RECIPIENTS; i++) {
			tooMany.add("" + i);
		}
		
		// This gets past one of the checks, but not the DAO's check
		try {
			createMessage(testUser, null, tooMany, null);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("not recognized"));
		}
		
		for (long i = MessageManagerImpl.MAX_NUMBER_OF_RECIPIENTS; i < MessageManagerImpl.MAX_NUMBER_OF_RECIPIENTS * 2; i++) {
			tooMany.add("" + i);
		}
		
		// This fails the manager's check
		try {
			createMessage(testUser, null, tooMany, null);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Consider grouping"));
		}
		
		// it's OK to do this as a trusted message sender
		try {
			createMessageWithThrottle(trustedMessageSender, null, tmsFileHandleId, tooMany, null);
			fail();
		} catch (IllegalArgumentException e) {
			// this shows that we got past the quantity and frequency limitations
			assertTrue(e.getMessage().contains("One or more of the following IDs are not recognized"));
		}
	}
	
	// can't create more than 10 messages in a minute
	// we test by creating this many plus one
	@Test(expected=TooManyRequestsException.class)
	public void testCreateTooFast() throws Exception {
		for (int i=0; i<11; i++) {
			MessageToUser m = createMessage(testUser, "userToOther", 
				new HashSet<String>() {{add(otherTestUser.getId().toString());}}, null);
			cleanup.add(m.getId());
		}

	}
	
	// can't create more than 10 messages in a minute
	// UNLESS WE'RE A TRUSTED MESSAGE SENDER (or a Synapse admin)
	@Test
	public void testCreateTooFastAsTrustedMessageSender() throws Exception {
		for (int i=0; i<11; i++) {
			MessageToUser m = createMessageWithThrottle(trustedMessageSender, "userToOther", tmsFileHandleId, 
				new HashSet<String>() {{add(trustedMessageSender.getId().toString());}}, null);
			cleanup.add(m.getId());
		}

	}
	
	@SuppressWarnings("serial")
	@Test
	public void testSendMessageSettings() throws Exception {
		final String testUserId = testUser.getId().toString();
		Set<String> testUserIdSet = new HashSet<String>() {{add(testUserId);}};
		
		// With default settings, the message should appear in the user's inbox
		MessageToUser message = createMessage(otherTestUser, "message1", testUserIdSet, null);
		List<MessageBundle> inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		
		// Emails are sent by default
		UserProfile profile = userProfileManager.getUserProfile(testUser.getId().toString());
		profile.setNotificationSettings(new Settings());
		profile.getNotificationSettings().setMarkEmailedMessagesAsRead(true);
		profile = userProfileManager.updateUserProfile(testUser, profile);
		
		// Now this second message will be marked as READ
		MessageToUser message2 = createMessage(otherTestUser, "message2", testUserIdSet, null);
		inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		inbox = messageManager.getInbox(testUser, 
				new ArrayList<MessageStatusType>() {{add(MessageStatusType.READ);}}, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message2, inbox.get(0).getMessage());
		
		// If you disable the sending of emails, the auto-READ-marking gets disabled too
		profile.getNotificationSettings().setSendEmailNotifications(false);
		profile = userProfileManager.updateUserProfile(testUser, profile);
		
		// Now the third message appears UNREAD
		MessageToUser message3 = createMessage(otherTestUser, "message3", testUserIdSet, null);
		inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message3, inbox.get(0).getMessage());
	}
	
	@Test
	public void testSendTemplateEmail() throws Exception {
		// Send an email to the test user
		messageManager.sendPasswordResetEmail(testUser.getId(), "Blah?");
		
		// Try the other one
		messageManager.sendWelcomeEmail(testUser.getId(), null);
		
		// Try the delivery failure email
		List<String> mockErrors = new ArrayList<String>();
		mockErrors.add(UUID.randomUUID().toString());
		messageManager.sendDeliveryFailureEmail(userToOther.getId(), mockErrors);
	}
	
	@Test
	public void testCreateMessageToEntityOwner() throws Exception {
		// Make an "entity"
		Node node = new Node();
		node.setName(UUID.randomUUID().toString());
		node.setNodeType(EntityType.project);
		nodeId = nodeManager.createNewNode(node, testUser);
		
		// Case #1 - Creator can share
		// This is in effect sending a message from the other test user to the test user
		userToOther.setRecipients(null);
		MessageToUser message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
		cleanup.add(message.getId());
		
		// Check the test user's inbox
		List<MessageBundle> inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		
		// Case #2 - Creator can't share
		// Have the admin give transfer the sharing permission to the other user
		AccessControlList acl = entityPermissionsManager.getACL(nodeId, adminUserInfo);
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(otherTestUser.getId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		ra.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl.getResourceAccess().add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
		
		// This is in effect sending a message from the other test user to itself
		userToOther.setRecipients(null);
		message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
		cleanup.add(message.getId());
		
		// Check the test user's inbox
		inbox = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		
		// Case #3 - Creator and other can share
		// Have the admin give sharing permission back to the creator
		acl = entityPermissionsManager.getACL(nodeId, adminUserInfo);
		ra = new ResourceAccess();
		ra.setPrincipalId(testUser.getId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>());
		ra.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl.getResourceAccess().add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
		
		// This is in effect sending a message from the other test user to the test user
		userToOther.setRecipients(null);
		message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
		cleanup.add(message.getId());
		
		// Check the test user's inbox
		inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		
		// Case #4 - Nobody can share
		acl = entityPermissionsManager.getACL(nodeId, adminUserInfo);
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		entityPermissionsManager.updateACL(acl, adminUserInfo);

		try {
			message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
			fail("Exception expected");
		} catch (UnauthorizedException e) { 
			// as expected
		}
	}
	
	@Test
	public void shareChildEntity() throws Exception {
		// Make an "entity"
		Node node = new Node();
		node.setName(UUID.randomUUID().toString());
		node.setNodeType(EntityType.project);
		nodeId = nodeManager.createNewNode(node, testUser);
		
		Node child = new Node();
		child.setName(UUID.randomUUID().toString());
		child.setNodeType(EntityType.project);
		child.setParentId(node.getId());
		childId = nodeManager.createNewNode(child, testUser);
		
		// Creator can share
		// This is in effect sending a message from the other test user to the test user
		userToOther.setRecipients(null);
		MessageToUser message = messageManager.createMessageToEntityOwner(otherTestUser, childId, userToOther);
		cleanup.add(message.getId());
		
		// Check the test user's inbox
		List<MessageBundle> inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.get(0).getMessage());
		
	}
	
	@Test
	public void testCreateMessageToInvalidRecipient() throws Exception {
		// No user has a negative ID (I hope)
		userToOther.getRecipients().add("-1");
		
		try {
			messageManager.createMessage(testUser, userToOther);
			fail();
		} catch (IllegalArgumentException e) {
			// We shouldn't get a nasty DB error
			assertFalse(e.getMessage().contains("foreign key"));
		}
	}
}
