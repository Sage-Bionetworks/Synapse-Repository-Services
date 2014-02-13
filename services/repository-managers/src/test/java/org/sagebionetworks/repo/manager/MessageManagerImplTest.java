package org.sagebionetworks.repo.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.migration.TestUtils;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

/**
 * Tests message access requirement checking and the sending of messages
 * Note: only the logic for sending messages is tested, a separate test handles tests of sending emails
 * 
 * Sorting of messages is not tested.  All tests order their results as most recent first.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageManagerImplTest {
	
	@Autowired
	private MessageManager messageManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private MembershipRequestManager membershipRequestManager;
	
	@Autowired
	private FileHandleDao fileDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	private FileHandleManager mockFileHandleManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = true;
	private static final long LIMIT = 100;
	private static final long OFFSET = 0;
	
	// Mutual spammers
	private UserInfo testUser;
	private UserInfo otherTestUser;
	private Team testTeam;
	
	private String fileHandleId;
	
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
	
	/**
	 * Note: This setup is very similar to {@link #DBOMessageDAOImplTest}
	 */
	@SuppressWarnings("serial")
	@Before
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		cleanup = new ArrayList<String>();
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setDomain(DomainType.SYNAPSE);
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		DBOCredential cred = new DBOCredential();
		cred.setAgreesToTermsOfUse(true);
		cred.setSecretKey("");
		
		// Need two users for this test
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

		final String testUserId = testUser.getId().toString();
		final String otherTestUserId = otherTestUser.getId().toString();

		// Create a team
		testTeam = new Team();
		testTeam.setName("MessageManagerImplTest");
		testTeam = teamManager.create(testUser, testTeam);
		final String testTeamId = testTeam.getId();
		
		// This user info needs to be updated to contain the team
		testUser = userManager.getUserInfo(testUser.getId());
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(testUserId);
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		// Mock out the file handle manager so that the fake file handle won't result in broken downloads
		mockFileHandleManager = mock(FileHandleManager.class);
		URL url = MessageManagerImplTest.class.getClassLoader().getResource("images/notAnImage.txt");
		when(mockFileHandleManager.getRedirectURLForFileHandle(anyString())).thenReturn(url);
		messageManager.setFileHandleManager(mockFileHandleManager);
		
		when(mockFileHandleManager.uploadFile(anyString(), any(FileItemStream.class))).thenReturn(handle);
		
		// Create all the messages
		// These will be send automatically since they have only one recipient
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
	
	/**
	 * Creates a message row
	 */
	private MessageToUser createMessage(UserInfo userInfo, String subject, Set<String> recipients, String inReplyTo) throws InterruptedException, NotFoundException {
		assertNotNull(userInfo);
		
		MessageToUser dto = new MessageToUser();
		// Note: ID is auto generated
		// Note: CreatedBy is derived from userInfo
		dto.setFileHandleId(fileHandleId);
		// Note: CreatedOn is set by the DAO
		dto.setSubject(subject);
		dto.setRecipients(recipients);
		dto.setInReplyTo(inReplyTo);
		// Note: InReplyToRoot is calculated by the DAO
		
		// Insert the message
		dto = messageManager.createMessage(userInfo, dto);
		assertNotNull(dto.getId());
		cleanup.add(dto.getId());
		assertEquals(userInfo.getId().toString(), dto.getCreatedBy());
		assertNotNull(dto.getCreatedOn());
		assertNotNull(dto.getInReplyToRoot());
		
		// Make sure the timestamps on the messages are different 
		Thread.sleep(5);
		
		return dto;
	}
	
	/**
	 * Sends the messages that must be sent by a worker
	 * 
	 * @param send_otherToSelfAndGroup This message may or may not have the proper permissions associated with it
	 */
	private List<String> sendUnsentMessages(boolean send_otherToGroup) throws Exception {
		assertEquals(0, messageManager.processMessage(userReplyToOtherAndSelf.getId()).size());
		assertEquals(0, messageManager.processMessage(otherReplyToUserAndSelf.getId()).size());
		assertEquals(0, messageManager.processMessage(userToSelfAndGroup.getId()).size());
		if (send_otherToGroup) {
			return messageManager.processMessage(otherToGroup.getId());
		}
		return new ArrayList<String>();
	}
	
	@After
	public void cleanup() throws Exception {
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
		
		// Reset the test user's notification settings to the default
		UserProfile profile = userProfileDAO.get(testUser.getId().toString());
		profile.setNotificationSettings(new Settings());
		userProfileDAO.update(profile);
		
		// Restore the old fileHandleManager
		messageManager.setFileHandleManager(fileHandleManager);
		
		userManager.deletePrincipal(adminUserInfo, testUser.getId());
		userManager.deletePrincipal(adminUserInfo, otherTestUser.getId());
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
		MessageToUser invisible = createMessage(otherTestUser, "This is a personal reminder", new HashSet<String>() {{add(otherTestUser.getId().toString());}}, null);
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
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(2L, messages.getTotalNumberOfResults());
		assertEquals(2, messages.getResults().size());
		assertEquals(forwarded, messages.getResults().get(0).getMessage());
		assertEquals(otherReplyToUser, messages.getResults().get(1).getMessage());
	}
	
	@Test
	public void testGetConversation_BeforeSending() throws Exception {
		QueryResults<MessageToUser> messages = messageManager.getConversation(testUser, userToOther.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3L, messages.getTotalNumberOfResults());
		assertEquals(3, messages.getResults().size());
		assertEquals(userReplyToOtherAndSelf, messages.getResults().get(0));
		assertEquals(otherReplyToUser, messages.getResults().get(1));
		assertEquals(userToOther, messages.getResults().get(2));
		
		messages = messageManager.getConversation(otherTestUser, otherReplyToUser.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3L, messages.getTotalNumberOfResults());
		assertEquals(3, messages.getResults().size());
		assertEquals(otherReplyToUserAndSelf, messages.getResults().get(0));
		assertEquals(otherReplyToUser, messages.getResults().get(1));
		assertEquals(userToOther, messages.getResults().get(2));
	}
	
	@Test
	public void testGetConversation_AfterSending() throws Exception {
		sendUnsentMessages(false);
		
		QueryResults<MessageToUser> messages = messageManager.getConversation(testUser, userToOther.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("All messages should have been sent", 4L, messages.getTotalNumberOfResults());
		assertEquals(4, messages.getResults().size());
		assertEquals(otherReplyToUserAndSelf, messages.getResults().get(0));
		assertEquals(userReplyToOtherAndSelf, messages.getResults().get(1));
		assertEquals(otherReplyToUser, messages.getResults().get(2));
		assertEquals(userToOther, messages.getResults().get(3));
		
		QueryResults<MessageToUser> whatTheOtherUserSees = messageManager.getConversation(otherTestUser, otherReplyToUser.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("The users have the same privileges regarding the thread's visibility", messages, whatTheOtherUserSees);
	}
	
	@Test
	public void testGetInbox_BeforeSending() throws Exception {
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
		assertEquals(otherReplyToUser, messages.getResults().get(0).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
		assertEquals(userToOther, messages.getResults().get(0).getMessage());
	}
	
	@Test
	public void testGetInbox_AfterSending() throws Exception {
		sendUnsentMessages(false);
		
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(4L, messages.getTotalNumberOfResults());
		assertEquals(4, messages.getResults().size());
		assertEquals(userToSelfAndGroup, messages.getResults().get(0).getMessage());
		assertEquals(otherReplyToUserAndSelf, messages.getResults().get(1).getMessage());
		assertEquals(userReplyToOtherAndSelf, messages.getResults().get(2).getMessage());
		assertEquals(otherReplyToUser, messages.getResults().get(3).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3L, messages.getTotalNumberOfResults());
		assertEquals(3, messages.getResults().size());
		assertEquals(otherReplyToUserAndSelf, messages.getResults().get(0).getMessage());
		assertEquals(userReplyToOtherAndSelf, messages.getResults().get(1).getMessage());
		assertEquals(userToOther, messages.getResults().get(2).getMessage());
	}
	
	@Test
	public void testGetOutbox() throws Exception {
		QueryResults<MessageToUser> messages = messageManager.getOutbox(testUser, 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("All sent messages should appear in the outbox regardless of sending status", 3L, messages.getTotalNumberOfResults());
		assertEquals(3, messages.getResults().size());
		assertEquals(userToSelfAndGroup, messages.getResults().get(0));
		assertEquals(userReplyToOtherAndSelf, messages.getResults().get(1));
		assertEquals(userToOther, messages.getResults().get(2));
		
		sendUnsentMessages(false);
		
		QueryResults<MessageToUser> afterSending = messageManager.getOutbox(testUser, 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(messages, afterSending);
	}
	
	@Test
	public void testSendMessage_DoesntFailOnResend() throws Exception {
		QueryResults<MessageBundle> messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
		
		messageManager.processMessage(userToOther.getId());
		messageManager.processMessage(userToOther.getId());
		messageManager.processMessage(userToOther.getId());
		
		// Multiple calls to sendMessage do nothing
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
	}
	
	@Test
	public void testSendMessage_NotAllowed() throws Exception {
		List<String> errors = sendUnsentMessages(true);
		assertEquals(1, errors.size());
		assertTrue(errors.get(0).contains("may not send"));
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
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(otherToGroup, messages.getResults().get(0).getMessage());
	}
	
	@Test
	public void testSendMessageTo_AUTH_USERS() throws Exception {
		// This should fail since no one has permission to send to this public group
		MessageToUser notAllowedToSend = createMessage(testUser, "I'm not allowed to do this", 
				Sets.newHashSet(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString()), null);
		List<String> errors = messageManager.processMessage(notAllowedToSend.getId());
		String joinedErrors = StringUtils.join(errors, "\n");
		assertTrue(joinedErrors.contains("may not send"));
		
		// But an admin can do it
		MessageToUser spam = createMessage(adminUserInfo, "I'm a malicious admin spammer!", 
				Sets.newHashSet(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString()), null);
		errors = messageManager.processMessage(spam.getId());
		assertEquals(StringUtils.join(errors, "\n"), 0, errors.size());
		
		// Now everyone has been spammed
		QueryResults<MessageBundle> messages = messageManager.getInbox(adminUserInfo, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(spam, messages.getResults().get(0).getMessage());
		
		messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(spam, messages.getResults().get(0).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(spam, messages.getResults().get(0).getMessage());
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
	}
	
	@SuppressWarnings("serial")
	@Test
	public void testSendMessageSettings() throws Exception {
		Set<String> testUserId = new HashSet<String>() {{add(testUser.getId().toString());}};
		
		// With default settings, the message should appear in the user's inbox
		MessageToUser message = createMessage(otherTestUser, "message1", testUserId, null);
		QueryResults<MessageBundle> inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.getResults().get(0).getMessage());
		
		// Emails are sent by default
		UserProfile profile = userProfileDAO.get(testUser.getId().toString());
		profile.setNotificationSettings(new Settings());
		profile.getNotificationSettings().setMarkEmailedMessagesAsRead(true);
		profile = userProfileDAO.update(profile);
		
		// Now this second message will be marked as READ
		MessageToUser message2 = createMessage(otherTestUser, "message2", testUserId, null);
		inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.getResults().get(0).getMessage());
		inbox = messageManager.getInbox(testUser, 
				new ArrayList<MessageStatusType>() {{add(MessageStatusType.READ);}}, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message2, inbox.getResults().get(0).getMessage());
		
		// If you disable the sending of emails, the auto-READ-marking gets disabled too
		profile.getNotificationSettings().setSendEmailNotifications(false);
		profile = userProfileDAO.update(profile);
		
		// Now the third message appears UNREAD
		MessageToUser message3 = createMessage(otherTestUser, "message3", testUserId, null);
		inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message3, inbox.getResults().get(0).getMessage());
	}
	
	@Test
	public void testSendTemplateEmail() throws Exception {
		// Send an email to the test user
		messageManager.sendPasswordResetEmail(testUser.getId(), DomainType.BRIDGE, "Blah?");
		
		// Try another variation
		messageManager.sendPasswordResetEmail(testUser.getId(), DomainType.SYNAPSE, "Blah?");
		
		// Try the other one
		messageManager.sendWelcomeEmail(testUser.getId(), DomainType.SYNAPSE);
		
		// Try the delivery failure email
		List<String> mockErrors = new ArrayList<String>();
		mockErrors.add(UUID.randomUUID().toString());
		messageManager.sendDeliveryFailureEmail(userToOther.getId(), mockErrors);
		
		// Try another variation
		messageManager.sendWelcomeEmail(testUser.getId(), DomainType.BRIDGE);
	}
	
	@Test
	public void testCreateMessageToEntityOwner() throws Exception {
		// Make an "entity"
		Node node = new Node();
		node.setName(UUID.randomUUID().toString());
		node.setNodeType(EntityType.getNodeTypeForClass(Project.class).name());
		nodeId = nodeManager.createNewNode(node, testUser);
		
		// Case #1 - Creator can share
		// This is in effect sending a message from the other test user to the test user
		userToOther.setRecipients(null);
		MessageToUser message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
		cleanup.add(message.getId());
		
		// Check the test user's inbox
		QueryResults<MessageBundle> inbox = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(message, inbox.getResults().get(0).getMessage());
		
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
		assertEquals(message, inbox.getResults().get(0).getMessage());
		
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
		assertEquals(message, inbox.getResults().get(0).getMessage());
		
		// Case #4 - Nobody can share
		acl = entityPermissionsManager.getACL(nodeId, adminUserInfo);
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		entityPermissionsManager.updateACL(acl, adminUserInfo);

		try {
			message = messageManager.createMessageToEntityOwner(otherTestUser, nodeId, userToOther);
		} catch (UnauthorizedException e) { }
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
