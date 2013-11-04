package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.RecipientType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMessageDAOImplTest {
	
	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private FileHandleDao fileDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	private String fileHandleId;
	
	private UserGroup maliciousUser;
	private UserGroup maliciousGroup;
	
	// These messages are named by their sender and receiver
	private Message userToUser;
	private Message userToGroup;
	private Message groupToUserAndGroup;
	private Message userCreateThread;
	private Message userToThread;
	private Message groupToThread;
	
	private List<MessageStatusType> unreadMessageInboxFilter = Arrays.asList(new MessageStatusType[] {  MessageStatusType.UNREAD });
	
	@Before
	public void spamMessages() throws Exception {
		// These two principals will act as mutual spammers
		maliciousUser = userGroupDAO.findGroup(AuthorizationConstants.TEST_USER_NAME, true);
		maliciousGroup = userGroupDAO.findGroup(AuthorizationConstants.TEST_GROUP_NAME, false);
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(maliciousUser.getId());
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		// Create all the messages
		userToUser = sendMessage(maliciousUser.getId(), null, "userToUser");
		userToGroup = sendMessage(maliciousUser.getId(), null, "userToGroup");
		groupToUserAndGroup = sendMessage(maliciousGroup.getId(), null, "groupToUserAndGroup");
		userCreateThread = sendMessage(maliciousUser.getId(), null, "userCreateThread");
		userToThread = sendMessage(maliciousUser.getId(), userCreateThread.getThreadId(), "userToThread");
		groupToThread = sendMessage(maliciousGroup.getId(), userCreateThread.getThreadId(), "groupToThread");
		
		// "Send" all the messages
		messageDAO.registerMessageRecipient(userToUser.getMessageId(), maliciousUser.getId());
		messageDAO.registerMessageRecipient(userToGroup.getMessageId(), maliciousGroup.getId());
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousUser.getId());
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousGroup.getId());
	}
	
	/**
	 * Creates a message row
	 * Message ID will be auto generated
	 * 
	 * @param userId The sender
	 * @param threadId Set to null to auto generate
	 * @param subject Arbitrary string, can be null
	 */
	@SuppressWarnings("serial")
	private Message sendMessage(String userId, String threadId, String subject) throws InterruptedException {
		assertNotNull(userId);
		
		Message dto = new Message();
		dto.setCreatedBy(userId);
		dto.setThreadId(threadId);
		dto.setSubject(subject);
		
		// These two fields will be eventually be used by a worker
		// to determine who to send messages to
		// For the sake of this test, the values do not matter (as long as they are non-null)
		dto.setRecipientType(RecipientType.PRINCIPAL);
		dto.setRecipients(new HashSet<String>() {{add("-1");}});
		
		dto.setMessageFileHandleId(fileHandleId);
		
		// Insert the row
		dto = messageDAO.createMessage(dto);
		assertNotNull(dto.getMessageId());
		assertNotNull(dto.getThreadId());
		assertNotNull(dto.getCreatedOn());
		
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
	public void testGetMessage() throws Exception {
		// All the created messages should exactly match the DTOs
		assertEquals(userToUser, messageDAO.getMessage(userToUser.getMessageId()));
		assertEquals(userToGroup, messageDAO.getMessage(userToGroup.getMessageId()));
		assertEquals(groupToUserAndGroup, messageDAO.getMessage(groupToUserAndGroup.getMessageId()));
		assertEquals(userCreateThread, messageDAO.getMessage(userCreateThread.getMessageId()));
		assertEquals(userToThread, messageDAO.getMessage(userToThread.getMessageId()));
		assertEquals(groupToThread, messageDAO.getMessage(groupToThread.getMessageId()));
	}
	
	@Test
	public void testGetThread_NoFilter_DescendSubject() throws Exception {
		assertEquals("There should be 3 messages in the thread", 3L, messageDAO.getThreadSize(userCreateThread.getThreadId()));
		
		// Test the user's visibility
		List<Message> thread = messageDAO.getThread(userCreateThread.getThreadId(), MessageSortBy.SUBJECT, true, 3, 0);
		assertEquals("Should get back 3 messages", 3, thread.size());
		
		// Order of messages should be descending alphabetical order by subject
		assertEquals(userToThread, thread.get(0));
		assertEquals(userCreateThread, thread.get(1));
		assertEquals(groupToThread, thread.get(2));
	}
	
	@Test
	public void testGetThread_DescendSubject() throws Exception {
		assertEquals("All messages belong to their own thread", 1L, messageDAO.getThreadSize(userToUser.getThreadId(), maliciousUser.getId()));
		assertEquals("All messages belong to their own thread", 1L, messageDAO.getThreadSize(userToGroup.getThreadId(), maliciousUser.getId()));
		assertEquals("All messages belong to their own thread", 1L, messageDAO.getThreadSize(groupToUserAndGroup.getThreadId(), maliciousGroup.getId()));
		assertEquals("There should be 2 messages in the thread visible to the user", 2L, messageDAO.getThreadSize(userToThread.getThreadId(), maliciousUser.getId()));
		assertEquals("There should be 1 message in the thread visible to the group", 1L, messageDAO.getThreadSize(groupToThread.getThreadId(), maliciousGroup.getId()));
		
		// Test the user's visibility
		List<Message> thread = messageDAO.getThread(userCreateThread.getThreadId(), maliciousUser.getId(), MessageSortBy.SUBJECT, true, 3, 0);
		assertEquals("Should get back 2 messages", 2, thread.size());
		
		// Order of messages should be descending alphabetical order by subject
		assertEquals(userToThread, thread.get(0));
		assertEquals(userCreateThread, thread.get(1));

		// Test the group's visibility
		thread = messageDAO.getThread(userCreateThread.getThreadId(), maliciousGroup.getId(), MessageSortBy.SUBJECT, true, 3, 0);
		assertEquals("Should get back 1 message", 1, thread.size());
		assertEquals(groupToThread, thread.get(0));
	}
	
	@Test
	public void testGetUserInbox_AscendDate() throws Exception {
		assertEquals("User has 2 messages", 2L, messageDAO.getNumReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter));
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter, MessageSortBy.SEND_DATE, false, 2, 0);
		assertEquals("Should get back all messages", 2, messages.size());
		
		// Order of messages should be ascending by creation time
		assertEquals(userToUser, messages.get(0).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
		assertEquals(groupToUserAndGroup, messages.get(1).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(1).getStatus().getStatus());
	}
	
	@Test
	public void testGetGroupInbox_DescendDate() throws Exception {
		assertEquals("Group has 2 messages", 2L, messageDAO.getNumReceivedMessages(maliciousGroup.getId(), unreadMessageInboxFilter));
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousGroup.getId(), unreadMessageInboxFilter, MessageSortBy.SEND_DATE, true, 2, 0);
		assertEquals("Should get back all messages", 2, messages.size());
		
		// Order of messages should be descending by creation time
		assertEquals(groupToUserAndGroup, messages.get(0).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
		assertEquals(userToGroup, messages.get(1).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(1).getStatus().getStatus());
	}
	
	@Test
	public void testGetUserOutbox_AscendSubject() throws Exception {
		assertEquals("User has sent 4 messages", 4L, messageDAO.getNumSentMessages(maliciousUser.getId()));
		
		List<Message> messages = messageDAO.getSentMessages(maliciousUser.getId(), MessageSortBy.SUBJECT, false, 4, 0);
		assertEquals("Should get back all messages", 4, messages.size());
		
		// Order of messages should be ascending by subject
		assertEquals(userCreateThread, messages.get(0));
		assertEquals(userToGroup, messages.get(1));
		assertEquals(userToThread, messages.get(2));
		assertEquals(userToUser, messages.get(3));
	}
	
	@Test
	public void testGetGroupOutbox_AscendDate() throws Exception {
		assertEquals("Group has sent 2 messages", 2L, messageDAO.getNumSentMessages(maliciousGroup.getId()));
		
		List<Message> messages = messageDAO.getSentMessages(maliciousGroup.getId(), MessageSortBy.SEND_DATE, false, 2, 0);
		assertEquals("Should get back all messages", 2, messages.size());
		
		// Order of messages should be ascending by time
		assertEquals(groupToUserAndGroup, messages.get(0));
		assertEquals(groupToThread, messages.get(1));
	}
	
	/**
	 * Mostly equivalent to {@link #testGetUserInbox_AscendDate()}
	 */
	@Test
	public void testUpdateMessageStatus() throws Exception {
		assertEquals("User has 2 messages", 2L, messageDAO.getNumReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter));
		
		// Change one message to READ
		messageDAO.updateMessageStatus(userToUser.getMessageId(), maliciousUser.getId(), MessageStatusType.READ);
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter, MessageSortBy.SEND_DATE, false, 2, 0);
		assertEquals("Should get back 1 message", 1, messages.size());
		
		// Order of messages should be ascending by creation time
		assertEquals(groupToUserAndGroup, messages.get(0).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
	}
}
