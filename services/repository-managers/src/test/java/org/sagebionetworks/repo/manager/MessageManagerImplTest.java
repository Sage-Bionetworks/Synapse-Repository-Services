package org.sagebionetworks.repo.manager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

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
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
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
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private FileHandleDao fileDAO;
	
	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = true;
	private static final long LIMIT = 100;
	private static final long OFFSET = 0;
	
	// Mutual spammers
	private UserInfo testUser;
	private UserGroup testGroup;
	private UserInfo otherTestUser;
	
	private String fileHandleId;
	
	@SuppressWarnings("serial")
	private final List<MessageStatusType> unreadMessageFilter = new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}};
	
	private MessageToUser userToOther;
	private MessageToUser otherReplyToUser;
	private MessageToUser userReplyToOtherAndSelf;
	private MessageToUser otherReplyToUserAndSelf;
	private MessageToUser userToSelfAndGroup;
	
	/**
	 * Note: This setup is very similar to {@link #DBOMessageDAOImplTest}
	 */
	@SuppressWarnings("serial")
	@Before
	public void setUp() throws Exception {
		testUser = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		testGroup = userGroupDAO.findGroup(AuthorizationConstants.TEST_GROUP_NAME, false);
		otherTestUser = userManager.getUserInfo(StackConfiguration.getIntegrationTestUserOneName());
		final String testUserId = testUser.getIndividualGroup().getId();
		final String testGroupId = testGroup.getId();
		final String otherTestUserId = otherTestUser.getIndividualGroup().getId();
		
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
				new HashSet<String>() {{add(testUserId); add(testGroupId);}}, null);
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
		assertEquals(userInfo.getIndividualGroup().getId(), dto.getCreatedBy());
		assertNotNull(dto.getCreatedOn());
		assertNotNull(dto.getInReplyToRoot());
		
		// Make sure the timestamps on the messages are different 
		Thread.sleep(2);
		
		return dto;
	}
	
	@After
	public void cleanup() throws Exception {
		// This will cascade delete all the messages generated for this test
		fileDAO.delete(fileHandleId);
	}
	
	@Test
	public void testGetConversation_BeforeSending() throws Exception {
		QueryResults<MessageToUser> messages = messageManager.getConversation(testUser, userToOther.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Only half of the messages should have been sent", 2L, messages.getTotalNumberOfResults());
		assertEquals(2, messages.getResults().size());
		assertEquals(otherReplyToUser, messages.getResults().get(0));
		assertEquals(userToOther, messages.getResults().get(1));
		
		QueryResults<MessageToUser> whatTheOtherUserSees = messageManager.getConversation(otherTestUser, otherReplyToUser.getId(), 
				SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("The users have the same privileges regarding the thread's visibility", messages, whatTheOtherUserSees);
	}
	
	@Test
	public void testGetInbox_BeforeSending() throws Exception {
		QueryResults<MessageBundle> messages = messageManager.getInbox(testUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Only half of the messages should have been sent", 1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
		assertEquals(otherReplyToUser, messages.getResults().get(0).getMessage());
		
		messages = messageManager.getInbox(otherTestUser, 
				unreadMessageFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals("Only half of the messages should have been sent", 1L, messages.getTotalNumberOfResults());
		assertEquals(1, messages.getResults().size());
		assertEquals(userToOther, messages.getResults().get(0).getMessage());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testSendMessage_NotIdempotent() throws Exception {
		messageManager.sendMessage(userToOther.getId());
	}
}
