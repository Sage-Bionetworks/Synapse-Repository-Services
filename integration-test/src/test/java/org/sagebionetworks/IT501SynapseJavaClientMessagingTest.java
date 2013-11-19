package org.sagebionetworks;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;

/**
 * Related to IT500SynapseJavaClient
 */
public class IT501SynapseJavaClientMessagingTest {

	private static final Long LIMIT = 100L;
	private static final Long OFFSET = 0L;

	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;

	private static String oneId;
	private static String twoId;

	private S3FileHandle oneToRuleThemAll;

	private MessageToUser oneToTwo;
	private MessageToUser twoToOne;

	@BeforeClass
	public static void beforeClass() throws Exception {
		synapseOne = SynapseClientHelper.createSynapseClient(
				StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		synapseTwo = SynapseClientHelper.createSynapseClient(
				StackConfiguration.getIntegrationTestUserTwoName(),
				StackConfiguration.getIntegrationTestUserTwoPassword());

		oneId = synapseOne.getMyProfile().getOwnerId();
		twoId = synapseTwo.getMyProfile().getOwnerId();
	}
	
	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		// Create a file handle to use with all the messages
		PrintWriter pw = null;
		File file = File.createTempFile("testEmailBody", ".txt");
		try {
			FileOutputStream fos = new FileOutputStream(file);
			pw = new PrintWriter(fos);
			pw.println("test");
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

		twoToOne = new MessageToUser();
		twoToOne.setFileHandleId(oneToRuleThemAll.getId());
		twoToOne.setRecipients(new HashSet<String>() {
			{
				add(oneId);
			}
		});
		twoToOne = synapseTwo.sendMessage(twoToOne);
	}

	@After
	public void after() throws Exception {
		synapseOne.deleteFileHandle(oneToRuleThemAll.getId());
	}

	@Test
	public void testGetInbox() throws Exception {
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(twoToOne, messages.getResults().get(0).getMessage());

		messages = synapseTwo.getInbox(null, null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0).getMessage());
	}

	@Test
	public void testGetOutbox() throws Exception {
		PaginatedResults<MessageToUser> messages = synapseOne.getOutbox(null,
				null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0));

		messages = synapseTwo.getOutbox(null, null, LIMIT, OFFSET);
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
		
		PaginatedResults<MessageBundle> messages = synapseOne.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(forwarded, messages.getResults().get(0).getMessage());
		assertEquals(twoToOne, messages.getResults().get(1).getMessage());
	}

	@Test
	public void testGetConversation() throws Exception {
		PaginatedResults<MessageToUser> ones = synapseOne.getConversation(oneToTwo.getId(), null, null, LIMIT, OFFSET);
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
}
