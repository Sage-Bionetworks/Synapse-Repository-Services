package org.sagebionetworks.message.workers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
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
	private SemaphoreManager semphoreManager;
	
	private UserInfo fromUserInfo;
	private UserInfo toUserInfo;
	private UserInfo adminUserInfo;
	
	private String fileHandleId;
	private MessageToUser message;
	
	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		fromUserInfo = userManager.getUserInfo(userManager.createUser(user));
		user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		toUserInfo = userManager.getUserInfo(userManager.createUser(user));
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		S3FileHandle handle = fileHandleManager.createCompressedFileFromString(fromUserInfo.getId().toString(), new Date(), "my dog has fleas");
		fileHandleId = handle.getId();
		
		message = new MessageToUser();
		message.setFileHandleId(fileHandleId);
		message.setRecipients(new HashSet<String>() {
			{
				add(toUserInfo.getId().toString());
				
				// Note: this causes the worker to send a delivery failure notification too
				// Which can be visually confirmed by the tester (appears in STDOUT)
				add(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString());
			}
		});
		message = messageManager.createMessage(fromUserInfo, message);
	}

	@After
	public void after() throws Exception {
		messageManager.deleteMessage(adminUserInfo, message.getId());
		
		fileHandleManager.deleteFileHandle(adminUserInfo, fileHandleId);
		
		userManager.deletePrincipal(adminUserInfo, fromUserInfo.getId());
		userManager.deletePrincipal(adminUserInfo, toUserInfo.getId());
	}
	
	
	@SuppressWarnings("serial")
	@Test
	public void testRoundTrip() throws Exception {
		List<MessageBundle> messages = null;
		
		long start = System.currentTimeMillis();
		while (messages == null || messages.size() < 1) {
			// Check the inbox of the recipient
			messages = messageManager.getInbox(toUserInfo, new ArrayList<MessageStatusType>() {
				{
					add(MessageStatusType.UNREAD);
				}
			}, MessageSortBy.SEND_DATE, true, 100, 0);
			
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for message to be sent", elapse < MAX_WAIT);
		}
		assertEquals(message, messages.get(0).getMessage());
	}
	
}
