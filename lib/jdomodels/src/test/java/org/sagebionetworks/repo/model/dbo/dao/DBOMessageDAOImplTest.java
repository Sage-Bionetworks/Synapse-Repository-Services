package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
	private MessageToUser userToUser;
	private MessageToUser userToUserAndGroup;
	private MessageToUser userToGroup;
	private MessageToUser groupReplyToUser;
	private MessageToUser userReplyToGroup;
	
	private List<MessageStatusType> unreadMessageInboxFilter = Arrays.asList(new MessageStatusType[] {  MessageStatusType.UNREAD });
	
	@SuppressWarnings("serial")
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
		userToUser = createMessage(maliciousUser.getId(), "userToUser", 
				new HashSet<String>() {{add(maliciousUser.getId());}}, null);
		userToUserAndGroup = createMessage(maliciousUser.getId(), "userToUserAndGroup", 
				new HashSet<String>() {{add(maliciousUser.getId()); add(maliciousGroup.getId());}}, null);
		userToGroup = createMessage(maliciousUser.getId(), "userToGroup", 
				new HashSet<String>() {{add(maliciousGroup.getId());}}, null);
		groupReplyToUser = createMessage(maliciousGroup.getId(), "groupReplyToUser", 
				new HashSet<String>() {{add(maliciousUser.getId());}}, userToGroup.getId());
		userReplyToGroup = createMessage(maliciousUser.getId(), "userReplyToGroup", 
				new HashSet<String>() {{add(maliciousGroup.getId());}}, groupReplyToUser.getId());
		
		// Send all the messages
		messageDAO.createMessageStatus(userToUser.getId(), maliciousUser.getId());
		messageDAO.createMessageStatus(userToUserAndGroup.getId(), maliciousUser.getId());
		messageDAO.createMessageStatus(userToUserAndGroup.getId(), maliciousGroup.getId());
		messageDAO.createMessageStatus(userToGroup.getId(), maliciousGroup.getId());
		messageDAO.createMessageStatus(groupReplyToUser.getId(), maliciousUser.getId());
		messageDAO.createMessageStatus(userReplyToGroup.getId(), maliciousGroup.getId());
	}
	
	/**
	 * Creates a message row
	 */
	private MessageToUser createMessage(String userId, String subject, Set<String> recipients, String inReplyTo) throws InterruptedException {
		assertNotNull(userId);
		
		MessageToUser dto = new MessageToUser();
		// Note: ID is auto generated
		dto.setCreatedBy(userId);
		dto.setFileHandleId(fileHandleId);
		// Note: CreatedOn is set by the DAO
		dto.setSubject(subject);
		dto.setRecipients(recipients);
		dto.setInReplyTo(inReplyTo);
		// Note: InReplyToRoot is calculated by the DAO
		
		// Insert the message
		dto = messageDAO.createMessage(dto);
		assertNotNull(dto.getId());
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
	public void testGetMessage() throws Exception {
		// All the created messages should exactly match the DTOs
		assertEquals(userToUser, messageDAO.getMessage(userToUser.getId()));
		assertEquals(userToUserAndGroup, messageDAO.getMessage(userToUserAndGroup.getId()));
		assertEquals(userToGroup, messageDAO.getMessage(userToGroup.getId()));
		assertEquals(groupReplyToUser, messageDAO.getMessage(groupReplyToUser.getId()));
		assertEquals(userReplyToGroup, messageDAO.getMessage(userReplyToGroup.getId()));
	}
	
	@Test
	public void testGetUserInbox_AscendDate() throws Exception {
		assertEquals("User has 3 messages", 3L, messageDAO.getNumReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter));
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousUser.getId(), 
				unreadMessageInboxFilter, MessageSortBy.SEND_DATE, false, 100, 0);
		assertEquals("Should get back all messages", 3, messages.size());
		
		// Order of messages should be ascending by creation time
		assertEquals(userToUser, messages.get(0).getMessage());
		assertEquals(userToUserAndGroup, messages.get(1).getMessage());
		assertEquals(groupReplyToUser, messages.get(2).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
		assertEquals(MessageStatusType.UNREAD, messages.get(1).getStatus().getStatus());
		assertEquals(MessageStatusType.UNREAD, messages.get(2).getStatus().getStatus());
	}
	
	@Test
	public void testGetGroupInbox_DescendDate() throws Exception {
		assertEquals("Group has 3 messages", 3L, messageDAO.getNumReceivedMessages(maliciousGroup.getId(), unreadMessageInboxFilter));
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousGroup.getId(), 
				unreadMessageInboxFilter, MessageSortBy.SEND_DATE, true, 100, 0);
		assertEquals("Should get back all messages", 3, messages.size());
		
		// Order of messages should be descending by creation time
		assertEquals(userReplyToGroup, messages.get(0).getMessage());
		assertEquals(userToGroup, messages.get(1).getMessage());
		assertEquals(userToUserAndGroup, messages.get(2).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
		assertEquals(MessageStatusType.UNREAD, messages.get(1).getStatus().getStatus());
		assertEquals(MessageStatusType.UNREAD, messages.get(2).getStatus().getStatus());
	}
	@Test
	public void testGetUserOutbox_AscendSubject() throws Exception {
		assertEquals("User has sent 4 messages", 4L, messageDAO.getNumSentMessages(maliciousUser.getId()));
		
		List<MessageToUser> messages = messageDAO.getSentMessages(maliciousUser.getId(), 
				MessageSortBy.SUBJECT, false, 100, 0);
		assertEquals("Should get back all messages", 4, messages.size());
		
		// Order of messages should be ascending by subject
		assertEquals(userReplyToGroup, messages.get(0));
		assertEquals(userToGroup, messages.get(1));
		assertEquals(userToUser, messages.get(2));
		assertEquals(userToUserAndGroup, messages.get(3));
	}
	
	@Test
	public void testGetGroupOutbox_AscendDate() throws Exception {
		assertEquals("Group has sent message", 1L, messageDAO.getNumSentMessages(maliciousGroup.getId()));
		
		List<MessageToUser> messages = messageDAO.getSentMessages(maliciousGroup.getId(), 
				MessageSortBy.SEND_DATE, false, 100, 0);
		assertEquals("Should get back the message", 1, messages.size());
		
		// Order of messages should be ascending by time
		assertEquals(groupReplyToUser, messages.get(0));
	}
	
	@Test
	public void testGetConversation_AscendDate() throws Exception {
		assertEquals("There are 3 messages in the conversation", 3L, messageDAO.getConversationSize(userToGroup.getId(), maliciousUser.getId()));
		
		List<MessageToUser> messages = messageDAO.getConversation(userToGroup.getId(), maliciousUser.getId(), 
				MessageSortBy.SEND_DATE, false, 100, 0);
		assertEquals("Should get back all messages", 3, messages.size());
		
		// Order of messages should be ascending by creation time
		assertEquals(userToGroup, messages.get(0));
		assertEquals(groupReplyToUser, messages.get(1));
		assertEquals(userReplyToGroup, messages.get(2));
	}
	
	@Test
	public void testGetConversation_Empty() throws Exception {
		assertEquals(0L, messageDAO.getConversationSize(userToUser.getId(), maliciousGroup.getId()));
		
		List<MessageToUser> messages = messageDAO.getConversation(userToUser.getId(), maliciousGroup.getId(), 
				MessageSortBy.SEND_DATE, true, 100, 0);
		assertEquals(0, messages.size());
	}

	
	@Test
	public void testUpdateMessageStatus() throws Exception {
		assertEquals("User has 3 unread messages", 3L, messageDAO.getNumReceivedMessages(maliciousUser.getId(), unreadMessageInboxFilter));
		
		// Get the original etag
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("messageId", userToUser.getId());
		DBOMessageContent content = basicDAO.getObjectByPrimaryKey(DBOMessageContent.class, params);
		String etag = content.getEtag();
		
		// Change one message to READ
		MessageStatus status = new MessageStatus();
		status.setMessageId(userToUser.getId());
		status.setRecipientId(maliciousUser.getId());
		status.setStatus(MessageStatusType.READ);
		messageDAO.updateMessageStatus(status);
		
		// Etag should have changed
		content = basicDAO.getObjectByPrimaryKey(DBOMessageContent.class, params);
		assertFalse(etag.equals(content.getEtag()));
		
		List<MessageBundle> messages = messageDAO.getReceivedMessages(maliciousUser.getId(), 
				unreadMessageInboxFilter, MessageSortBy.SEND_DATE, false, 100, 0);
		assertEquals("Should get back 2 messages", 2, messages.size());
		
		// Order of messages should be ascending by creation time
		assertEquals(userToUserAndGroup, messages.get(0).getMessage());
		assertEquals(groupReplyToUser, messages.get(1).getMessage());
		assertEquals(MessageStatusType.UNREAD, messages.get(0).getStatus().getStatus());
		assertEquals(MessageStatusType.UNREAD, messages.get(1).getStatus().getStatus());
	}
}
