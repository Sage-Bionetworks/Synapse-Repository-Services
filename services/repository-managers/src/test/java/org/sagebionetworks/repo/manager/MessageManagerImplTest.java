package org.sagebionetworks.repo.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.migration.TestUtils;
import org.sagebionetworks.repo.manager.team.MembershipRequestManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests message access requirement checking
 * and the sending of messages
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
	
	/**
	 * Note: This setup is very similar to {@link #DBOMessageDAOImplTest}
	 */
	@SuppressWarnings("serial")
	@Before
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
		cleanup = new ArrayList<String>();
		
		testUser = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		otherTestUser = userManager.getUserInfo(StackConfiguration.getIntegrationTestUserOneName());
		final String testUserId = testUser.getIndividualGroup().getId();
		final String otherTestUserId = otherTestUser.getIndividualGroup().getId();
		
		// Create a team
		testTeam = new Team();
		testTeam.setName("MessageManagerImplTest");
		testTeam = teamManager.create(testUser, testTeam);
		final String testTeamId = testTeam.getId();
		
		// This user info needs to be updated to contain the team
		testUser = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(testUserId);
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
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
				new HashSet<String>() {{add(otherTestUserId); add(testTeamId);}}, null);
	}
	
	/**
	 * Creates a message row
	 */
	private MessageToUser createMessage(UserInfo userInfo, String subject, Set<String> recipients, String inReplyTo) throws InterruptedException {
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
		assertEquals(userInfo.getIndividualGroup().getId(), dto.getCreatedBy());
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
		assertEquals(0, messageManager.sendMessage(userReplyToOtherAndSelf.getId(), false).size());
		assertEquals(0, messageManager.sendMessage(otherReplyToUserAndSelf.getId(), false).size());
		assertEquals(0, messageManager.sendMessage(userToSelfAndGroup.getId(), false).size());
		if (send_otherToGroup) {
			return messageManager.sendMessage(otherToGroup.getId(), false);
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
	}
	
	@SuppressWarnings("serial")
	@Test(expected = IllegalArgumentException.class) 
	public void testSendMessageToMultipleRecipientsInOneTransaction() throws Exception {
		MessageToUser multipleRecipients = createMessage(testUser,
				"multipleRecipients", new HashSet<String>() {
					{
						add(testUser.getIndividualGroup().getId());
						add(otherTestUser.getIndividualGroup().getId());
					}
				}, null);
		messageManager.sendMessage(multipleRecipients.getId(), true);
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
		MessageToUser invisible = createMessage(otherTestUser, "This is a personal reminder", new HashSet<String>() {{add(otherTestUser.getIndividualGroup().getId());}}, null);
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
		final String testUserId = testUser.getIndividualGroup().getId();
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
		
		messageManager.sendMessage(userToOther.getId(), false);
		messageManager.sendMessage(userToOther.getId(), false);
		messageManager.sendMessage(userToOther.getId(), false);
		
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
		request.setUserId(otherTestUser.getIndividualGroup().getId());
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
	
	@SuppressWarnings("serial")
	@Test
	public void testSendMessageTo_AUTH_USERS() throws Exception {
		// Find the ID of the AUTHENTICATED_USERS group
		String findingAuthUsers = null;
		for (UserGroup ug : testUser.getGroups()) {
			if (ug.getName().equals(DEFAULT_GROUPS.AUTHENTICATED_USERS.toString())) {
				findingAuthUsers = ug.getId();
			}
		}
		assertNotNull(findingAuthUsers);
		final String authUsersId = findingAuthUsers;
		
		// This should fail since no one has permission to send to this public group
		MessageToUser notAllowedToSend = createMessage(testUser, "I'm not allowed to do this", new HashSet<String>() {{add(authUsersId);}}, null);
		List<String> errors = messageManager.sendMessage(notAllowedToSend.getId(), false);
		String joinedErrors = StringUtils.join(errors, "\n");
		System.out.println(joinedErrors);
		assertTrue(joinedErrors.contains("may not send"));
	}
}
