package org.sagebionetworks;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;

/**
 * Related to IT500SynapseJavaClient
 */
public class IT501SynapseJavaClientMessagingTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;
	private static Long user1ToDelete;
	private static Long user2ToDelete;

	private static final Long LIMIT = 100L;
	private static final Long OFFSET = 0L;
	private static final String MESSAGE_BODY = "Blah blah blah\n";

	private static String oneId;
	private static String twoId;

	private S3FileHandle oneToRuleThemAll;

	private MessageToUser oneToTwo;
	private MessageToUser twoToOne;
	
	private List<String> cleanup;
	private Project project;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapseOne = new SynapseClientImpl();
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseTwo = new SynapseClientImpl();
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo);
	
		oneId = synapseOne.getMyProfile().getOwnerId();
		twoId = synapseTwo.getMyProfile().getOwnerId();
	}
	
	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		cleanup = new ArrayList<String>();
		
		// Create a file handle to use with all the messages
		PrintWriter pw = null;
		File file = File.createTempFile("testEmailBody", ".txt");
		try {
			FileOutputStream fos = new FileOutputStream(file);
			pw = new PrintWriter(fos);
			pw.print(MESSAGE_BODY);
			pw.close();
			pw = null;
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
		oneToRuleThemAll = synapseOne.createFileHandle(file, "text/plain");

		oneToTwo = new MessageToUser();
		oneToTwo.setFileHandleId(oneToRuleThemAll.getId());
		oneToTwo.setRecipients(new HashSet<String>() {
			{
				add(twoId);
			}
		});
		oneToTwo = synapseOne.sendMessage(oneToTwo);
		cleanup.add(oneToTwo.getId());

		twoToOne = new MessageToUser();
		twoToOne.setFileHandleId(oneToRuleThemAll.getId());
		twoToOne.setRecipients(new HashSet<String>() {
			{
				add(oneId);
			}
		});
		twoToOne.setInReplyTo(oneToTwo.getId());
		twoToOne = synapseTwo.sendMessage(twoToOne);
		cleanup.add(twoToOne.getId());
	}

	@After
	public void after() throws Exception {
		for (String id : cleanup) {
			adminSynapse.deleteMessage(id);
		}
		
		if (project != null) {
			try {
				adminSynapse.deleteAndPurgeEntityById(project.getId());
			} catch (Exception e) { }
		}
		
		try {
			synapseOne.deleteFileHandle(oneToRuleThemAll.getId());
		} catch (Exception e) { }
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(user1ToDelete);
		adminSynapse.deleteUser(user2ToDelete);
	}

	@SuppressWarnings("serial")
	@Test
	public void testGetInbox() throws Exception {
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(twoToOne, messages.getResults().get(0).getMessage());

		messages = synapseTwo.getInbox(new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}}, null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0).getMessage());
	}

	@Test
	public void testGetOutbox() throws Exception {
		PaginatedResults<MessageToUser> messages = synapseOne.getOutbox(null,
				null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0));

		messages = synapseTwo.getOutbox(null, true, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(twoToOne, messages.getResults().get(0));
	}

	@Test
	public void testGetMessage() throws Exception {
		assertEquals(oneToTwo, synapseOne.getMessage(oneToTwo.getId()));
		assertEquals(twoToOne, synapseOne.getMessage(twoToOne.getId()));
		
		assertEquals(oneToTwo, synapseTwo.getMessage(oneToTwo.getId()));
		assertEquals(twoToOne, synapseTwo.getMessage(twoToOne.getId()));
	}

	@Test
	public void testForwardMessage() throws Exception {
		MessageRecipientSet recipients = new MessageRecipientSet();
		recipients.setRecipients(twoToOne.getRecipients());
		MessageToUser forwarded = synapseOne.forwardMessage(oneToTwo.getId(), recipients);
		cleanup.add(forwarded.getId());
		
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(2, messages.getResults().size());
		assertEquals(forwarded, messages.getResults().get(0).getMessage());
		assertEquals(twoToOne, messages.getResults().get(1).getMessage());
	}

	@Test
	public void testGetConversation() throws Exception {
		PaginatedResults<MessageToUser> ones = synapseOne.getConversation(oneToTwo.getId(), MessageSortBy.SEND_DATE, null, LIMIT, OFFSET);
		assertEquals(2, ones.getResults().size());
		assertEquals(twoToOne, ones.getResults().get(0));
		assertEquals(oneToTwo, ones.getResults().get(1));
		
		PaginatedResults<MessageToUser> twos = synapseTwo.getConversation(twoToOne.getId(), null, null, LIMIT, OFFSET);
		assertEquals(ones, twos);
	}

	@Test
	public void testUpdateStatus() throws Exception {
		MessageStatus status = new MessageStatus();
		status.setMessageId(twoToOne.getId());
		status.setStatus(MessageStatusType.READ);
		synapseOne.updateMessageStatus(status);
		
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(0, messages.getResults().size());
	}
	
	@Test
	public void testTooManyMessages() throws Exception {
		// DDOS (not really) the messaging service
		long start = System.currentTimeMillis();
		boolean gotNerfed = false;
		int i;
		for (i = 1; i <= 100; i++) {
			try {
				oneToTwo = synapseOne.sendMessage(oneToTwo);
				cleanup.add(oneToTwo.getId());
			} catch (SynapseUserException e) {
				if (e.getMessage().contains("429")) {
					gotNerfed = true;
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		
		assertTrue(
				"Assuming that a service calls takes less than 6 seconds to complete, we should have hit the rate limit.  A total of "
						+ i + " messages were sent in " + (end - start) + " ms.  Average: " + ((end - start) / i) + " ms",
				gotNerfed);
	}
	
	@Test
	public void testDownloadMessage() throws Exception {
		String message = synapseTwo.downloadMessage(oneToTwo.getId());
		
		assertTrue("Downloaded: " + message, MESSAGE_BODY.equals(message));
	}
	
	@Test
	public void testSendMessageToEntityOwner() throws Exception {
		project = synapseOne.createEntity(new Project());
		
		// Send a message from two to one in a different way
		twoToOne.setRecipients(null);
		MessageToUser message = synapseTwo.sendMessage(twoToOne, project.getId());
		cleanup.add(message.getId());
		
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				MessageSortBy.SEND_DATE, true, LIMIT, OFFSET);
		assertEquals(message, messages.getResults().get(0).getMessage());
	}
}
