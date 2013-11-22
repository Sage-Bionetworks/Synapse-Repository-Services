package org.sagebionetworks.message.workers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageReceiver;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.QueryResults;
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
	private FileHandleDao fileDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private MessageReceiver messageToUserQueueMessageReveiver;
	
	private UserInfo userInfo;
	private String fileHandleId;
	private MessageToUser message;
	
	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		// Before we start, make sure the queue is empty
		emptyQueue();
		
		userInfo = userManager.getUserInfo(AuthorizationConstants.TEST_USER_NAME);
		final UserInfo otherInfo = userManager.getUserInfo(StackConfiguration.getIntegrationTestUserOneName());

		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName("bucketName");
		handle.setKey("key");
		handle.setContentType("content type");
		handle.setContentSize(123l);
		handle.setContentMd5("md5");
		handle.setCreatedBy(userInfo.getIndividualGroup().getId());
		handle.setFileName("foobar.txt");
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
		
		message = new MessageToUser();
		message.setFileHandleId(fileHandleId);
		message.setRecipients(new HashSet<String>() {
			{
				add(userInfo.getIndividualGroup().getId());
				add(otherInfo.getIndividualGroup().getId());
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
			count = messageToUserQueueMessageReveiver.triggerFired();
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
		// UserInfo adminUserInfo = userManager.getUserInfo(AuthorizationConstants.ADMIN_USER_NAME);
		// messageManager.deleteMessage(adminUserInfo, message.getId());
		
		fileDAO.delete(fileHandleId);
	}
	
	
	@SuppressWarnings("serial")
	@Test
	public void testRoundTrip() throws Exception {
		QueryResults<MessageBundle> messages = null;
		
		long start = System.currentTimeMillis();
		while (messages == null || messages.getResults().size() < 1) {
			messages = messageManager.getInbox(userInfo, new ArrayList<MessageStatusType>() {
				{
					add(MessageStatusType.UNREAD);
				}
			}, MessageSortBy.SEND_DATE, true, 100, 0);
			
			System.out.println("Waiting for message to be sent...");
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for message to be sent", elapse < MAX_WAIT);
		}
		
		assertEquals(message, messages.getResults().get(0).getMessage());
	}
	
}
