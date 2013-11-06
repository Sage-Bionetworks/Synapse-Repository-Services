package org.sagebionetworks.repo.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.migration.TestUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.RecipientType;
import org.sagebionetworks.repo.web.NotFoundException;
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
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private FileHandleDao fileDAO;
	
	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = true;
	private static final long LIMIT = 10;
	private static final long OFFSET = 0;
	
	// Mutual spammers
	private UserInfo testUser;
	private UserInfo otherTestUser;
	
	private String fileHandleId;
	private String testUserNodeId;
	
	@SuppressWarnings("serial")
	private final List<MessageStatusType> unreadMessageFilter = new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}};
	
	/**
	 * This is a message the user can see from both inbox and outbox
	 */
	private Message userToUser;
	
	/**
	 * This is a message the user can see from the outbox
	 * The other user can see this from the inbox
	 * Both can see it in a thread
	 */
	private Message userToOther;
	
	/**
	 * This is a message the user can see from the inbox
	 * The other user can see this from the outbox
	 * Both can see it in a thread
	 */
	private Message otherReplyToUser;
	
	/**
	 * This is a message the user can see from the inbox
	 * The other user can see this from both inbox and outbox
	 */
	private Message otherToUserAndSelf;
	
	/**
	 * This is a comment on the user's entity
	 */
	private Message userCreateThread;
	
	/**
	 * This is another comment on the user's entity
	 * The user forgets to specify the thread ID, 
	 * but the manager should be smart enough to fill that value in
	 */
	private Message userToThread;
	
	/**
	 * This is a comment that is unsendable to the user's entity
	 * Unless a special privilege is given
	 */
	private Message otherToThread;
	
	/**
	 * Initializes the otherToThread message in the same pattern as the other messages
	 */
	@SuppressWarnings("serial")
	private void initOtherToThreadMessage() throws Exception {
		otherToThread = sendMessage(otherTestUser, userCreateThread.getThreadId(), "otherToThread", 
				RecipientType.ENTITY, new HashSet<String>() {{add(testUserNodeId);}});
	}
	
	/**
	 * Note: This setup is very similar to {@link #DBOMessageDAOImplTest}
	 */
	@SuppressWarnings({ "serial", "deprecation" })
	@Before
	public void setUp() throws Exception {
		testUser = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		otherTestUser = userManager.getUserInfo(StackConfiguration.getIntegrationTestUserOneName());
		final String testUserId = testUser.getIndividualGroup().getId();
		final String otherTestUserId = otherTestUser.getIndividualGroup().getId();
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(testUserId);
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		// This entity is created by and therefore visible to the test user
		Node newNode = new Node();
		newNode.setName("MessageManagerTest");
		newNode.setNodeType(EntityType.project.name());
		testUserNodeId = nodeManager.createNewNode(newNode, testUser);
		
		// Create all the messages, but don't send them yet
		userToUser = sendMessage(testUser, null, "userToUser", 
				RecipientType.PRINCIPAL, new HashSet<String>() {{add(testUserId);}});
		userToOther = sendMessage(testUser, null, "userToOther", 
				RecipientType.PRINCIPAL, new HashSet<String>() {{add(otherTestUserId);}});
		otherReplyToUser = sendMessage(otherTestUser, userToOther.getThreadId(), "otherReplyToUser", 
				RecipientType.PRINCIPAL, new HashSet<String>() {{add(testUserId);}});
		otherToUserAndSelf = sendMessage(otherTestUser, null, "otherToUserAndSelf", 
				RecipientType.PRINCIPAL, new HashSet<String>() {{add(testUserId); add(otherTestUserId);}});
		userCreateThread = sendMessage(testUser, null, "userCreateThread", 
				RecipientType.ENTITY, new HashSet<String>() {{add(testUserNodeId);}});
		userToThread = sendMessage(testUser, null, "userToThread", 
				RecipientType.ENTITY, new HashSet<String>() {{add(testUserNodeId);}});
		// Note: the groupToThread message is not initialized since the group does not have access to the entity
	}
	
	/**
	 * Creates a message row
	 * Message ID will be auto generated
	 * 
	 * @param userId The sender
	 * @param threadId Set to null to auto generate
	 * @param subject Arbitrary string, can be null
	 * @throws NotFoundException 
	 */
	private Message sendMessage(UserInfo userInfo, String threadId, String subject, 
			RecipientType rType, Set<String> recipients) throws Exception {
		assertNotNull(userInfo);
		assertNotNull(rType);
		assertNotNull(recipients);
		assertTrue(recipients.size() > 0);
		
		Message dto = new Message();
		dto.setThreadId(threadId);
		dto.setSubject(subject);
		dto.setRecipientType(rType);
		dto.setRecipients(recipients);
		
		// Common file handle
		dto.setMessageFileHandleId(fileHandleId);
		
		// The manager should make sure that the "createdBy" field is filled in
		dto = messageManager.createMessage(userInfo, dto);
		assertNotNull(dto.getMessageId());
		assertNotNull(dto.getThreadId());
		assertEquals(userInfo.getIndividualGroup().getId(), dto.getCreatedBy());
		assertNotNull(dto.getCreatedOn());
		
		// Make sure the timestamps are different
		Thread.sleep(2);
		
		return dto;
	}
	
	@After
	public void cleanup() throws Exception {
		// Linked to thread IDs
		nodeManager.delete(testUser, testUserNodeId);
		
		// This will cascade delete all the messages generated for this test
		fileDAO.delete(fileHandleId);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAutoFillThreadIdOnCreate() throws Exception {
		// The thread ID is set to null in the creation of the userToThread object
		// Since the recipient is an entity, the thread must match, 
		// and the manager should fill it in in case the user forgets.
		assertEquals(userCreateThread.getThreadId(), userToThread.getThreadId());
		
		// If the thread ID is specified, then the create should fail
		userToThread.setThreadId("-1");
		messageManager.createMessage(testUser, userToThread);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetCommentThreadOnMessageThread() throws Exception {
		// This method does not filter by sender/receiver, so it cannot be used for non-comment threads
		messageManager.getCommentThread(testUser, userToUser.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
	}
	
	@Test
	public void testGetCommentThreadForUser() throws Exception {
		// This should return the correct set of results 
		// even though the messages have not been "sent" yet
		// because the user is the sender
		QueryResults<Message> messages = messageManager.getCommentThread(testUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		testGetCommentThread_AssertStatements(messages);
		
		messageManager.sendMessage(userCreateThread.getMessageId());
		messageManager.sendMessage(userToThread.getMessageId());
		
		// Repeating the test after sending should yield the same results
		messages = messageManager.getCommentThread(testUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		testGetCommentThread_AssertStatements(messages);
	}
	
	/**
	 * Helper for {@link #testGetCommentThreadForUser()}
	 * and for {@link #testGetCommentThreadForGroup()}
	 */
	private void testGetCommentThread_AssertStatements(QueryResults<Message> messages) {
		assertEquals("There are two messages in the thread", 2L, messages.getTotalNumberOfResults());
		assertEquals("There are two messages in the thread", 2, messages.getResults().size());
		assertEquals(userToThread, messages.getResults().get(0));
		assertEquals(userCreateThread, messages.getResults().get(1));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetCommentThreadForOther_Fail() throws Exception {
		messageManager.getCommentThread(otherTestUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
	}
	
	@Test
	public void testGetCommentThreadForOther() throws Exception {
		// The test user allows the other user to read the entity
		AccessControlList acl = entityPermissionsManager.getACL(testUserNodeId, testUser);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, otherTestUser.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, testUser);
		
		// Now the results should be equivalent to what the test user sees
		QueryResults<Message> messages = messageManager.getCommentThread(otherTestUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		testGetCommentThread_AssertStatements(messages);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testPostToCommentThread_Fail() throws Exception {
		initOtherToThreadMessage();
	}
	
	@Test
	public void testPostToCommentThread() throws Exception {
		// The test user allows the other user to read and post to the entity
		AccessControlList acl = entityPermissionsManager.getACL(testUserNodeId, testUser);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, otherTestUser.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = AuthorizationTestHelper.addToACL(acl, otherTestUser.getIndividualGroup(), ACCESS_TYPE.SEND_MESSAGE);
		acl = entityPermissionsManager.updateACL(acl, testUser);
		
		// Now this should work
		initOtherToThreadMessage();

		QueryResults<Message> messages = messageManager.getCommentThread(testUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("There are now three messages in the thread", 3L, messages.getTotalNumberOfResults());
		assertEquals("There are now three messages in the thread", 3, messages.getResults().size());
		assertEquals(otherToThread, messages.getResults().get(0));
		assertEquals(userToThread, messages.getResults().get(1));
		assertEquals(userCreateThread, messages.getResults().get(2));
		
		QueryResults<Message> whatTheOtherUserSees = messageManager.getCommentThread(testUser, userCreateThread.getThreadId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("The users have the same privileges regarding the entity's comment thread visibility", messages, whatTheOtherUserSees);
	}
	
	@Test
	public void testGetMessageThread() throws Exception {
		QueryResults<Message> messages = messageManager.getMessageThread(testUser, userToOther.getThreadId(), testUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("User can only see the sent message", 1L, messages.getTotalNumberOfResults());
		assertEquals("User can only see the sent message", 1, messages.getResults().size());
		assertEquals(userToOther, messages.getResults().get(0));
		
		// Ignoring the fact that you can't reply to a message you can't see :)
		messages = messageManager.getMessageThread(otherTestUser, otherReplyToUser.getThreadId(), otherTestUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Other user can only see the sent message", 1L, messages.getTotalNumberOfResults());
		assertEquals("Other user can only see the sent message", 1, messages.getResults().size());
		assertEquals(otherReplyToUser, messages.getResults().get(0));
		
		// Send the messages
		messageManager.sendMessage(userToOther.getMessageId());
		messageManager.sendMessage(otherReplyToUser.getMessageId());
		
		messages = messageManager.getMessageThread(testUser, userToOther.getThreadId(), testUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Now both messages are visible", 2L, messages.getTotalNumberOfResults());
		assertEquals("Now both messages are visible", 2, messages.getResults().size());
		assertEquals(otherReplyToUser, messages.getResults().get(0));
		assertEquals(userToOther, messages.getResults().get(1));
		
		QueryResults<Message> whatTheOtherUserSees = messageManager.getMessageThread(otherTestUser, otherReplyToUser.getThreadId(), otherTestUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("The users have the same privileges regarding the thread's visibility", messages, whatTheOtherUserSees);
	}
	
	@Test
	public void testGetInbox_ChronologicalSending() throws Exception {
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, testUser.getIndividualGroup().getId(), 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Nothing has been sent yet", 0L, messages.getTotalNumberOfResults());
		assertEquals("Nothing has been sent yet", 0, messages.getResults().size());
		
		QueryResults<MessageBundle> whatTheOtherUserSees = messageManager.getInbox(otherTestUser, otherTestUser.getIndividualGroup().getId(), 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Nothing has been sent yet", messages, whatTheOtherUserSees);

		// Send all messages except the uninitialized one (otherUserToThread)
		// Note: the send order is in the order of creation, which is the simplest case
		messageManager.sendMessage(userToUser.getMessageId());
		messageManager.sendMessage(userToOther.getMessageId());
		messageManager.sendMessage(otherReplyToUser.getMessageId());
		messageManager.sendMessage(otherToUserAndSelf.getMessageId());
		messageManager.sendMessage(userCreateThread.getMessageId());
		messageManager.sendMessage(userToThread.getMessageId());
		
		// Now the inboxes diverge in their contents
		messages = messageManager.getInbox(testUser, testUser.getIndividualGroup().getId(), 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(3L, messages.getTotalNumberOfResults());
		assertEquals(3, messages.getResults().size());
		assertEquals(otherToUserAndSelf, messages.getResults().get(0).getMessage());
		assertEquals(otherReplyToUser, messages.getResults().get(1).getMessage());
		assertEquals(userToUser, messages.getResults().get(2).getMessage());
		
		whatTheOtherUserSees = messageManager.getInbox(otherTestUser, otherTestUser.getIndividualGroup().getId(), 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(2L, whatTheOtherUserSees.getTotalNumberOfResults());
		assertEquals(2, whatTheOtherUserSees.getResults().size());
		assertEquals(otherToUserAndSelf, whatTheOtherUserSees.getResults().get(0).getMessage());
		assertEquals(userToOther, whatTheOtherUserSees.getResults().get(1).getMessage());
	}
	
	@Test
	public void testGetOutbox_UnchangedAfterSending_ReverseSendOrder() throws Exception {
		QueryResults<Message> messages = messageManager.getOutbox(testUser, testUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		QueryResults<Message> whatTheOtherUserSees = messageManager.getOutbox(otherTestUser, otherTestUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		tesGetOutbox_AssertStatements(messages, whatTheOtherUserSees);

		// Send all messages except the uninitialized one (otherUserToThread)
		// Note: the send order is in the reverse order of creation
		messageManager.sendMessage(userToThread.getMessageId());
		messageManager.sendMessage(userCreateThread.getMessageId());
		messageManager.sendMessage(otherToUserAndSelf.getMessageId());
		messageManager.sendMessage(otherReplyToUser.getMessageId());
		messageManager.sendMessage(userToOther.getMessageId());
		messageManager.sendMessage(userToUser.getMessageId());
		
		// Outboxes are unchanged
		messages = messageManager.getOutbox(testUser, testUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		whatTheOtherUserSees = messageManager.getOutbox(otherTestUser, otherTestUser.getIndividualGroup().getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		tesGetOutbox_AssertStatements(messages, whatTheOtherUserSees);
	}
	
	/**
	 * Helper for {@link #testGetOutbox_UnchangedAfterSending()}
	 */
	private void tesGetOutbox_AssertStatements(QueryResults<Message> messages, QueryResults<Message> whatTheOtherUserSees) {
		assertEquals(4L, messages.getTotalNumberOfResults());
		assertEquals(4, messages.getResults().size());
		assertEquals(userToThread, messages.getResults().get(0));
		assertEquals(userCreateThread, messages.getResults().get(1));
		assertEquals(userToOther, messages.getResults().get(2));
		assertEquals(userToUser, messages.getResults().get(3));
		
		assertEquals(2L, whatTheOtherUserSees.getTotalNumberOfResults());
		assertEquals(2, whatTheOtherUserSees.getResults().size());
		assertEquals(otherToUserAndSelf, whatTheOtherUserSees.getResults().get(0));
		assertEquals(otherReplyToUser, whatTheOtherUserSees.getResults().get(1));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSendMessage_NotIdempotent() throws Exception {
		messageManager.sendMessage(userToUser.getMessageId());
		messageManager.sendMessage(userToUser.getMessageId());
	}
}
