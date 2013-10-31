package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;

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
	
	private String fileHandleId;
	
	// These messages are by their sender and receiver
	private Message userToUser;
	private Message userToGroup;
	private Message groupToUserAndGroup;
	private Message userCreateThread;
	private Message userToThread;
	private Message groupToThread;
	
	@Before
	public void spamEmails() throws Exception {
		// These two principals will act as mutual spammers
		UserGroup maliciousUser = userGroupDAO.findGroup(AuthorizationConstants.TEST_USER_NAME, true);
		UserGroup maliciousGroup = userGroupDAO.findGroup(AuthorizationConstants.TEST_GROUP_NAME, false);
		
		// We need a file handle to satisfy a foreign key constraint
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = TestUtils.createS3FileHandle(maliciousUser.getId());
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		// Create all the messages...
		userToUser = sendMessage(maliciousUser.getId(), null, "userToUser");
		messageDAO.registerMessageRecipient(userToUser.getMessageId(), maliciousUser.getId());
		
		userToGroup = sendMessage(maliciousUser.getId(), null, "userToGroup");
		messageDAO.registerMessageRecipient(userToGroup.getMessageId(), maliciousGroup.getId());
		
		groupToUserAndGroup = sendMessage(maliciousGroup.getId(), null, "groupToUserAndGroup");
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousUser.getId());
		messageDAO.registerMessageRecipient(groupToUserAndGroup.getMessageId(), maliciousGroup.getId());
		
		userCreateThread = sendMessage(maliciousUser.getId(), null, "userCreateThread");
		userToThread = sendMessage(maliciousUser.getId(), userCreateThread.getThreadId(), "userToThread");
		groupToThread = sendMessage(maliciousGroup.getId(), userCreateThread.getThreadId(), "groupToThread");
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
	private Message sendMessage(String userId, String threadId, String subject) {
		assertNotNull(userId);
		
		Message dto = new Message();
		dto.setCreatedBy(userId);
		dto.setThreadId(threadId);
		dto.setSubject(subject);
		
		// These two fields will be eventually be used by a worker
		// to determine who to send messages to
		// For the sake of this test, the values do not matter (as long as they are non-null)
		dto.setRecipientType(RecipientType.PRINCIPAL);
		dto.setRecipients(new ArrayList<String>() {{add("-1");}});
		
		dto.setMessageFileId(fileHandleId);
		dto.setCreatedOn(new Date());
		
		// Insert the row
		messageDAO.saveMessage(dto);
		assertNotNull(dto.getMessageId());
		return dto;
	}
	
	@After
	public void cleanup() throws Exception {
		// This will cascade delete all the messages generated for this test
		fileDAO.delete(fileHandleId);
	}
	
	@Test
	public void testGetMessage() throws Exception {
		
	}
}
