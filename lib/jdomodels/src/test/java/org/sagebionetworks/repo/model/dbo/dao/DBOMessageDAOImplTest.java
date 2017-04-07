package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
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
	
	@Autowired
	private DBOChangeDAO changeDAO;

	@Autowired
	private IdGenerator idGenerator;
	
	private String fileHandleId;
	
	private UserGroup maliciousUser;
	private UserGroup maliciousGroup;
	
	// These messages are named by their sender and receiver
	private MessageToUser userToUser;
	private MessageToUser userToUserAndGroup;
	private MessageToUser userToGroup;
	private MessageToUser groupReplyToUser;
	private MessageToUser userReplyToGroup;
	
	private List<String> cleanup;
	
	private List<MessageStatusType> unreadMessageInboxFilter = Arrays.asList(new MessageStatusType[] {  MessageStatusType.UNREAD });
	
	@SuppressWarnings("serial")
	@Before
	public void spamMessages() throws Exception {
		cleanup = new ArrayList<String>();
		
		changeDAO.deleteAllChanges();
		
		// These two principals will act as mutual spammers
		maliciousUser = new UserGroup();
		maliciousUser.setIsIndividual(true);
		maliciousUser.setId(userGroupDAO.create(maliciousUser).toString());

		maliciousGroup = new UserGroup();
		maliciousGroup.setIsIndividual(false);
		maliciousGroup.setId(userGroupDAO.create(maliciousGroup).toString());
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(maliciousUser.getId(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle = (S3FileHandle) fileDAO.createFile(handle);
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
		messageDAO.createMessageStatus_SameTransaction(userToUser.getId(), maliciousUser.getId(), null);
		messageDAO.createMessageStatus_NewTransaction(userToUserAndGroup.getId(), maliciousUser.getId(), null);
		messageDAO.createMessageStatus_SameTransaction(userToUserAndGroup.getId(), maliciousGroup.getId(), null);
		messageDAO.createMessageStatus_NewTransaction(userToGroup.getId(), maliciousGroup.getId(), null);
		messageDAO.createMessageStatus_SameTransaction(groupReplyToUser.getId(), maliciousUser.getId(), null);
		messageDAO.createMessageStatus_NewTransaction(userReplyToGroup.getId(), maliciousGroup.getId(), null);
		
		// to simulate sending we also have to set the 'sent' flag to 'true'
		messageDAO.updateMessageTransmissionAsComplete(userToUser.getId());
		messageDAO.updateMessageTransmissionAsComplete(userToUserAndGroup.getId());
		messageDAO.updateMessageTransmissionAsComplete(userToGroup.getId());
		messageDAO.updateMessageTransmissionAsComplete(groupReplyToUser.getId());
		messageDAO.updateMessageTransmissionAsComplete(userReplyToGroup.getId());
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
		String unsubEndpoint = "https://www.synapse.org/#foo:";
		String userProfileSettingEndpoint = "https://www.synapse.org/#Profile:edit";
		dto.setNotificationUnsubscribeEndpoint(unsubEndpoint);
		dto.setUserProfileSettingEndpoint(userProfileSettingEndpoint);
		dto.setWithUnsubscribeLink(true);
		dto.setWithProfileSettingLink(false);
		dto.setIsNotificationMessage(true);
		// Note: InReplyToRoot is calculated by the DAO
		String to = "Foo<foo@sb.com>";
		dto.setTo(to);
		String cc = "Bar<bar@sb.com>";
		dto.setCc(cc);
		String bcc = "Baz<baz@sb.com>";
		dto.setBcc(bcc);
		
		// Insert the message
		dto = messageDAO.createMessage(dto);
		assertNotNull(dto.getId());
		cleanup.add(dto.getId());
		
		// make sure its created properly
		assertNotNull(dto.getCreatedOn());
		assertNotNull(dto.getInReplyToRoot());
		assertEquals(userId, dto.getCreatedBy());
		assertEquals(fileHandleId, dto.getFileHandleId());
		assertEquals(inReplyTo, dto.getInReplyTo());
		assertEquals(unsubEndpoint, dto.getNotificationUnsubscribeEndpoint());
		assertEquals(userProfileSettingEndpoint, dto.getUserProfileSettingEndpoint());
		assertTrue(dto.getWithUnsubscribeLink());
		assertTrue(dto.getIsNotificationMessage());
		assertFalse(dto.getWithProfileSettingLink());
		assertEquals(recipients, dto.getRecipients());
		assertEquals(subject, dto.getSubject());
		assertEquals(to, dto.getTo());
		assertEquals(cc, dto.getCc());
		assertEquals(bcc, dto.getBcc());
		
		// make sure 'getMessage' returns the same thing
		MessageToUser clone = messageDAO.getMessage(dto.getId());
		assertEquals(dto, clone);
		
		// Make sure the timestamps on the messages are different 
		Thread.sleep(2);
		
		return dto;
	}
	
	@After
	public void cleanup() throws Exception {
		for (String id : cleanup) {
			messageDAO.deleteMessage(id);
		}
		fileDAO.delete(fileHandleId);
		userGroupDAO.delete(maliciousUser.getId());
		userGroupDAO.delete(maliciousGroup.getId());
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
	
	@Test(expected=NotFoundException.class)
	public void testGetNonexistentMessage() throws Exception {
		messageDAO.getMessage("-1");
	}
	
	@Test
	public void testChangeMessageGenerated() throws Exception {
		List<ChangeMessage> changes = changeDAO.listUnsentMessages(1000);
		
		// Look for one of the messages
		for (ChangeMessage change : changes) {
			if (ObjectType.MESSAGE == change.getObjectType() && userToUser.getId().equals(change.getObjectId())) {
				return;
			}
		}
		fail("Change message was not created for a message");
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
		messageDAO.updateMessageStatus_SameTransaction(status);
		
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
	
	@Test
	public void testHasMessageBeenSent() throws Exception {
		assertTrue(messageDAO.getMessageSent(userToUser.getId()));
		assertTrue(messageDAO.getMessageSent(userToUserAndGroup.getId()));
		assertTrue(messageDAO.getMessageSent(userToGroup.getId()));
		assertTrue(messageDAO.getMessageSent(groupReplyToUser.getId()));
		assertTrue(messageDAO.getMessageSent(userReplyToGroup.getId()));
	}
	
	@Test
	public void testCanCreateMessage() throws Exception {
		// Note: The malicious user has already created at least 3 messages
		
		// Default settings
		assertTrue(messageDAO.canCreateMessage(maliciousUser.getId(), 10, 60000));
		
		// Negative interval
		assertTrue(messageDAO.canCreateMessage(maliciousUser.getId(), 1, -1));
		
		// Super long interval with low threshold (hopefully the test takes less than 1 hour :)
		assertFalse(messageDAO.canCreateMessage(maliciousUser.getId(), 1, 3600000));
		
		// Super long interval with normal threshold
		assertTrue(messageDAO.canCreateMessage(maliciousUser.getId(), 10, 3600000));
		
		// Negative threshold
		assertFalse(messageDAO.canCreateMessage(maliciousUser.getId(), -1, 60000));
		
		// Negative threshold takes priority over negative interval
		assertFalse(messageDAO.canCreateMessage(maliciousUser.getId(), -1, -1));
	}
	
	@Test
	public void testCanSeeMessagesUsingFileHandle() throws Exception {
		Set<Long> groups = new HashSet<Long>();
		
		// An empty collection should not see anything
		assertFalse(messageDAO.canSeeMessagesUsingFileHandle(groups, fileHandleId));
		
		// Non existent users should not see anything
		groups.add(-1L);
		assertFalse(messageDAO.canSeeMessagesUsingFileHandle(groups, fileHandleId));
		
		// The malicious user has been sent a message with the file handle
		groups.add(Long.parseLong(maliciousUser.getId()));
		assertTrue(messageDAO.canSeeMessagesUsingFileHandle(groups, fileHandleId));
		
		// So has the malicious group
		groups.clear();
		groups.add(Long.parseLong(maliciousGroup.getId()));
		assertTrue(messageDAO.canSeeMessagesUsingFileHandle(groups, fileHandleId));
		
		// Having both in the list should work too
		groups.add(Long.parseLong(maliciousUser.getId()));
		assertTrue(messageDAO.canSeeMessagesUsingFileHandle(groups, fileHandleId));

		// Shouldn't be able to see an unrelated filehandle
		assertFalse(messageDAO.canSeeMessagesUsingFileHandle(groups, "-1"));
	}
	
	@Test
	public void testNotAsciiSubject() throws Exception {
		userToUser = createMessage(maliciousUser.getId(), "non-ascii subject ライブで行ったことのな", 
				new HashSet<String>() {{add(maliciousUser.getId());}}, null);
	}
	
	
}
