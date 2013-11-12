package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageInReplyToRoot;
import org.sagebionetworks.repo.model.dbo.persistence.DBOComment;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.tools.javac.util.Pair;

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
	
	private String threadId;
	
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
		userToUser = createMessage(maliciousUser.getId(), "userToUser");
		userToGroup = createMessage(maliciousUser.getId(), "userToGroup");
		groupToUserAndGroup = createMessage(maliciousGroup.getId(), "groupToUserAndGroup");
		userCreateThread = createMessage(maliciousUser.getId(), "userCreateThread");
		userToThread = createMessage(maliciousUser.getId(), "userToThread");
		groupToThread = createMessage(maliciousGroup.getId(), "groupToThread");
		
		// Send the first three messages
		messageDAO.registerMessageRecipient(userToUser.getMessageId(), maliciousUser.getId());
		messageDAO.registerMessageRecipient(userToGroup.getMessageId(), maliciousGroup.getId());
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousUser.getId());
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousGroup.getId());
		
		// Assign the last three messages to the same thread
		threadId = messageDAO.registerMessageToThread(userCreateThread.getMessageId(), null);
		messageDAO.registerMessageToThread(userToThread.getMessageId(), threadId);
		messageDAO.registerMessageToThread(groupToThread.getMessageId(), threadId);
	}
	
	/**
	 * Creates a message row
	 * Message ID will be auto generated
	 * 
	 * @param userId The sender
	 * @param subject Arbitrary string, can be null
	 */
	@SuppressWarnings("serial")
	private Message createMessage(String userId, String subject) throws InterruptedException {
		assertNotNull(userId);
		
		Message dto = new Message();
		dto.setCreatedBy(userId);
		dto.setSubject(subject);
		
		// These two fields will be eventually be used by a worker
		// to determine who to send messages to
		// For the sake of this test, the values do not matter (as long as they are non-null)
		dto.setRecipientType(ObjectType.PRINCIPAL);
		dto.setRecipients(new HashSet<String>() {{add("-1");}});
		
		dto.setMessageFileHandleId(fileHandleId);
		
		// Insert the row
		dto = messageDAO.createMessage(dto);
		assertNotNull(dto.getMessageId());
		assertNotNull(dto.getCreatedOn());
		
		// Make sure the timestamps on the messages are different 
		Thread.sleep(2);
		
		return dto;
	}
	
	@After
	public void cleanup() throws Exception {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("threadId", threadId);
		basicDAO.deleteObjectByPrimaryKey(DBOComment.class, params);
		
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
		assertEquals("There should be 3 messages in the thread", 3L, messageDAO.getThreadSize(threadId));
		
		// Test the user's visibility
		List<Message> thread = messageDAO.getThread(threadId, MessageSortBy.SUBJECT, true, 3, 0);
		assertEquals("Should get back 3 messages", 3, thread.size());
		
		// Order of messages should be descending alphabetical order by subject
		assertEquals(userToThread, thread.get(0));
		assertEquals(userCreateThread, thread.get(1));
		assertEquals(groupToThread, thread.get(2));
	}
	
	@Test
	public void testGetThread_DescendSubject() throws Exception {
		assertEquals("There should be 2 messages in the thread visible to the user", 2L, messageDAO.getThreadSize(threadId, maliciousUser.getId()));
		assertEquals("There should be 1 message in the thread visible to the group", 1L, messageDAO.getThreadSize(threadId, maliciousGroup.getId()));
		
		// Test the user's visibility
		List<Message> thread = messageDAO.getThread(threadId, maliciousUser.getId(), MessageSortBy.SUBJECT, true, 3, 0);
		assertEquals("Should get back 2 messages", 2, thread.size());
		
		// Order of messages should be descending alphabetical order by subject
		assertEquals(userToThread, thread.get(0));
		assertEquals(userCreateThread, thread.get(1));

		// Test the group's visibility
		thread = messageDAO.getThread(threadId, maliciousGroup.getId(), MessageSortBy.SUBJECT, true, 3, 0);
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
	
	@Test
	public void testGetThreadId() throws Exception {
		assertEquals("Thread ID should be fetchable from message ID", threadId, messageDAO.getMessageThread(userCreateThread.getMessageId())); 
		assertEquals("Thread ID should be fetchable from message ID", threadId, messageDAO.getMessageThread(userToThread.getMessageId()));
		assertEquals("Thread ID should be fetchable from message ID", threadId, messageDAO.getMessageThread(groupToThread.getMessageId()));
	}
	
	@Test
	public void testThreadToObjectLinkage() throws Exception {
		try {
			messageDAO.getObjectOfThread(threadId);
			fail();
		} catch (NotFoundException e) { }
		
		ObjectType oType = ObjectType.ENTITY;
		String oId = "-1";
		
		messageDAO.registerThreadToObject(threadId, oType, oId);
		List<String> threads = messageDAO.getThreadsOfObject(oType, oId);
		assertEquals("Should be one thread", 1, threads.size());
		assertEquals("Should be the only thread", threadId, threads.get(0));
		Pair<ObjectType, String> obj = messageDAO.getObjectOfThread(threadId);
		assertEquals(oType, obj.fst);
		assertEquals(oId, obj.snd);
	}
	
	@Test
	public void testDeleteCascadeForNonUniqueForeignKey() throws Exception {
		ObjectType oType = ObjectType.ENTITY;
		String oId = "-1";
		
		// Register the thread to an object
		messageDAO.registerThreadToObject(threadId, oType, oId);
		
		// Check the registration
		List<String> threads = messageDAO.getThreadsOfObject(oType, oId);
		assertEquals("Should be one thread", 1, threads.size());
		assertEquals("Should be the only thread", threadId, threads.get(0));
		
		// Delete one of the messages from the bigger thread
		// The foreign key should prevent the delete
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("childMessageId", groupToThread.getMessageId());
		try {
			basicDAO.deleteObjectByPrimaryKey(DBOMessageInReplyToRoot.class, params);
			fail();
		} catch (DataIntegrityViolationException e) { }
	}
}
