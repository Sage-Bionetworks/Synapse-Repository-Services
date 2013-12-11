package org.sagebionetworks.message.workers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test validates that messages to users pushed to the topic propagate to the proper queue,
 * and are then processed by the worker and "sent" to users.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageToUserWorkerIntegrationTest {
	
	public static final long MAX_WAIT = 60 * 1000; // one minute
	
	@Autowired
	private MessageManager messageManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private MessageReceiver messageToUserQueueMessageReceiver;
	
	private UserInfo userInfo;
	private UserInfo otherUserInfo;
	private String fileHandleId;
	private MessageToUser message;
	
	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue is empty
		emptyQueue();
		
		userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		otherUserInfo = userManager.getUserInfo(StackConfiguration.getIntegrationTestUserOneName());

		final URL url = MessageToUserWorkerIntegrationTest.class.getClassLoader().getResource("Message.txt");
		FileItemStream fis = new FileItemStream() {

			@Override
			public InputStream openStream() throws IOException {
				return url.openStream();
			}

			@Override
			public String getContentType() {
				return "application/text";
			}

			@Override
			public String getName() {
				return "Message.txt";
			}

			@Override
			public String getFieldName() {
				return "none";
			}

			@Override
			public boolean isFormField() {
				return false;
			}
			
		};
		S3FileHandle handle = fileHandleManager.uploadFile(userInfo.getIndividualGroup().getId(), fis);
		fileHandleId = handle.getId();
		
		message = new MessageToUser();
		message.setFileHandleId(fileHandleId);
		message.setRecipients(new HashSet<String>() {
			{
				add(otherUserInfo.getIndividualGroup().getId());
				
				// Note: this causes the worker to send a delivery failure notification too
				// Which can be visually confirmed by the tester (appears in STDOUT)
				add(userGroupDAO.findGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false).getId());
			}
		});
		message = messageManager.createMessage(userInfo, message);
	}

	/**
	 * Empty the queue by processing all messages on the queue.
	 */
	public void emptyQueue() throws InterruptedException {
		long start = System.currentTimeMillis();
		int count = 0;
		do {
			count = messageToUserQueueMessageReceiver.triggerFired();
			System.out.println("Emptying the message (to user) queue, there were at least: "
							+ count + " messages on the queue");
			Thread.yield();
			long elapse = System.currentTimeMillis() - start;
			if (elapse > MAX_WAIT * 2)
				throw new RuntimeException(
						"Timed out waiting process all messages that were on the queue before the tests started.");
		} while (count > 0);
	}

	@After
	public void after() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
		messageManager.deleteMessage(adminUserInfo, message.getId());
		
		fileHandleManager.deleteFileHandle(adminUserInfo, fileHandleId);
	}
	
	
	@SuppressWarnings("serial")
	@Test
	public void testRoundTrip() throws Exception {
		QueryResults<MessageBundle> messages = null;
		
		long start = System.currentTimeMillis();
		while (messages == null || messages.getResults().size() < 1) {
			// Check the inbox of the recipient
			messages = messageManager.getInbox(otherUserInfo, new ArrayList<MessageStatusType>() {
				{
					add(MessageStatusType.UNREAD);
				}
			}, MessageSortBy.SEND_DATE, true, 100, 0);
			
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for message to be sent", elapse < MAX_WAIT);
		}
		assertEquals(message, messages.getResults().get(0).getMessage());
	}
	
}
