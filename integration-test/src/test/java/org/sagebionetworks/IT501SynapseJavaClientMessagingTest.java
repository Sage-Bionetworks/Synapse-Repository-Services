package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseServerException;
import org.sagebionetworks.client.exceptions.SynapseTooManyRequestsException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;

/**
 * Related to IT500SynapseJavaClient
 */
@ExtendWith(ITTestExtension.class)
public class IT501SynapseJavaClientMessagingTest {

	private static SynapseClient synapseTwo;
	private static Long user2ToDelete;

	private static final Long LIMIT = 100L;
	private static final Long OFFSET = 0L;
	private static final String MESSAGE_BODY = "Blah blah blah\n";
	private static final String MESSAGE_BODY_WITH_EXTENDED_CHARS = "G\u00E9rard Depardieum, Camille Saint-Sa\u00EBns and Myl\u00E8ne Demongeot";
	private static final long MAX_MESSAGE_WAIT = 60 * 1000;

	private static String twoId;

	private static FileHandle oneToRuleThemAll;
	
	private static String synapseUserId;
	
	private String fileHandleIdWithExtendedChars;

	private MessageToUser oneToTwo;
	private MessageToUser twoToOne;
	
	private List<String> cleanup;
	private static Project project;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public IT501SynapseJavaClientMessagingTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse, SynapseClient synapse) throws Exception {
		synapseUserId = synapse.getMyProfile().getOwnerId();
		// Create second user
		synapseTwo = new SynapseClientImpl();
		user2ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseTwo);
	
		twoId = synapseTwo.getMyProfile().getOwnerId();
		
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
		project = synapse.createEntity(new Project());
		oneToRuleThemAll = synapse.multipartUpload(file, null, false, false);
	}
	
	@BeforeEach
	public void before() throws Exception {
		adminSynapse.clearAllLocks();
		cleanup = new ArrayList<String>();

		oneToTwo = new MessageToUser();
		oneToTwo.setFileHandleId(oneToRuleThemAll.getId());
		oneToTwo.setRecipients(Collections.singleton(twoId));
		oneToTwo = synapse.sendMessage(oneToTwo);
		cleanup.add(oneToTwo.getId());
		
		waitForMessage(synapseTwo, oneToTwo.getId());

		twoToOne = new MessageToUser();
		twoToOne.setFileHandleId(oneToRuleThemAll.getId());
		twoToOne.setRecipients(Collections.singleton(synapseUserId));
		twoToOne.setInReplyTo(oneToTwo.getId());
		twoToOne = synapseTwo.sendMessage(twoToOne);
		cleanup.add(twoToOne.getId());
		
		waitForMessage(synapse, twoToOne.getId());

		fileHandleIdWithExtendedChars = null;
	}

	/**
	 * Wait for the message with the given id to reach the inbox of the given client with an UNREAD status
	 * 
	 * @param client
	 * @param messageId
	 * @throws Exception
	 */
	private void waitForMessage(SynapseClient client, String messageId) throws Exception {
		long start = System.currentTimeMillis();
		
		while (true) {
			
			if (System.currentTimeMillis() - start >= MAX_MESSAGE_WAIT) {
				throw new IllegalStateException("Timed out waiting for message: " + messageId);
			}
			
			System.out.println("Waiting for message (" + messageId + ") to reach inbox...");
			
			PaginatedResults<MessageBundle> inbox = client.getInbox(Collections.singletonList(MessageStatusType.UNREAD), MessageSortBy.SEND_DATE, true, LIMIT, OFFSET);
		
			if (!inbox.getResults().isEmpty() && inbox.getResults().get(0).getMessage().getId().equals(messageId)) {
				return;
			}
			
			Thread.sleep(1000);
			
		}
	}
	
	@AfterEach
	public void after() throws Exception {
		for (String id : cleanup) {
			adminSynapse.deleteMessage(id);
		}
		
		try {
			if (fileHandleIdWithExtendedChars!=null) {
				adminSynapse.deleteFileHandle(fileHandleIdWithExtendedChars);
			}
			fileHandleIdWithExtendedChars = null;
		} catch (SynapseException e) { }
	}
	
	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		// Delete the file handle
		try {
			adminSynapse.deleteFileHandle(oneToRuleThemAll.getId());
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteEntity(project);
		} catch (SynapseException e) { }
	}

	@SuppressWarnings("serial")
	@Test
	public void testGetInbox() throws Exception {
		PaginatedResults<MessageBundle> messages = synapse.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(twoToOne, messages.getResults().get(0).getMessage());

		messages = synapseTwo.getInbox(new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}}, null, null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0).getMessage());
	}

	@Test
	public void testGetOutbox() throws Exception {
		PaginatedResults<MessageToUser> messages = synapse.getOutbox(null,
				null, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(oneToTwo, messages.getResults().get(0));

		messages = synapseTwo.getOutbox(null, true, LIMIT, OFFSET);
		assertEquals(1, messages.getResults().size());
		assertEquals(twoToOne, messages.getResults().get(0));
	}

	@Test
	public void testGetMessage() throws Exception {
		assertEquals(oneToTwo, synapse.getMessage(oneToTwo.getId()));
		assertEquals(twoToOne, synapse.getMessage(twoToOne.getId()));
		
		assertEquals(oneToTwo, synapseTwo.getMessage(oneToTwo.getId()));
		assertEquals(twoToOne, synapseTwo.getMessage(twoToOne.getId()));
	}

	@Test
	public void testForwardMessage() throws Exception {
		MessageRecipientSet recipients = new MessageRecipientSet();
		recipients.setRecipients(twoToOne.getRecipients());
		MessageToUser forwarded = synapse.forwardMessage(oneToTwo.getId(), recipients);
		cleanup.add(forwarded.getId());
		
		waitForMessage(synapse, forwarded.getId());
		
		PaginatedResults<MessageBundle> messages = synapse.getInbox(null,
				null, null, LIMIT, OFFSET);
		assertEquals(2, messages.getResults().size());
		assertEquals(forwarded, messages.getResults().get(0).getMessage());
		assertEquals(twoToOne, messages.getResults().get(1).getMessage());
	}

	@Test
	public void testGetConversation() throws Exception {
		PaginatedResults<MessageToUser> ones = synapse.getConversation(oneToTwo.getId(), MessageSortBy.SEND_DATE, null, LIMIT, OFFSET);
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
		synapse.updateMessageStatus(status);
		
		PaginatedResults<MessageBundle> messages = synapse.getInbox(null,
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
				oneToTwo = synapse.sendMessage(oneToTwo);
				cleanup.add(oneToTwo.getId());
			} catch (SynapseServerException e) {
				if (e instanceof SynapseTooManyRequestsException) {
					gotNerfed = true;
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		
		assertTrue(gotNerfed, 
				"Assuming that a service calls takes less than 6 seconds to complete, we should have hit the rate limit.  A total of "
						+ i + " messages were sent in " + (end - start) + " ms.  Average: " + ((end - start) / i) + " ms");
	}
	
	@Test
	public void testDownloadMessageToFile() throws Exception {
		File temp = File.createTempFile("test", null);
		temp.deleteOnExit();
		synapseTwo.downloadMessageToFile(oneToTwo.getId(), temp);
		// now compare the downloaded file to the message body
		FileInputStream f = new FileInputStream( temp );
		int b;
		byte[] bytes = new byte[(int)temp.length()];
		int i = 0;
		try {
			while ( (b=f.read()) != -1 ) bytes[i++]=(byte)b;
		} finally {
			f.close();
		}
		assertEquals(new String(bytes, "utf-8"), MESSAGE_BODY);
	}
	
	@Test
	public void testDownloadMessage() throws Exception {
		String message = synapseTwo.downloadMessage(oneToTwo.getId());
		
		assertTrue(MESSAGE_BODY.equals(message), "Downloaded: " + message);
	}
	
	@Test
	public void testGetMessageTemporaryUrl() throws Exception {
		String url = synapseTwo.getMessageTemporaryUrl(oneToTwo.getId());
		// just test that it's a valid URL (will throw exception if not)
		new URL(url);
	}
	
	@Test
	public void testDownloadExtendedCharacterMessage() throws Exception {
		MessageToUser mtu = new MessageToUser();
		mtu.setSubject("a test");
		mtu.setRecipients(new HashSet<String>(Arrays.asList(new String[]{twoId})));
		mtu = synapse.sendStringMessage(mtu, MESSAGE_BODY_WITH_EXTENDED_CHARS);
		cleanup.add(mtu.getId());
		this.fileHandleIdWithExtendedChars = mtu.getFileHandleId();

		// this inspects the content-type to determine the character encoding
		String message = synapseTwo.downloadMessage(mtu.getId());
		
		assertTrue(MESSAGE_BODY_WITH_EXTENDED_CHARS.equals(message), "Downloaded: " + message);
	}
	
	@Test
	public void testSendMessageToEntityOwner() throws Exception {
		// Send a message from two to one in a different way
		twoToOne.setRecipients(null);
		MessageToUser message = synapseTwo.sendMessage(twoToOne, project.getId());
		cleanup.add(message.getId());
		
		waitForMessage(synapse, message.getId());
		
		PaginatedResults<MessageBundle> messages = synapse.getInbox(null,
				MessageSortBy.SEND_DATE, true, LIMIT, OFFSET);
		assertEquals(message, messages.getResults().get(0).getMessage());
	}
}
