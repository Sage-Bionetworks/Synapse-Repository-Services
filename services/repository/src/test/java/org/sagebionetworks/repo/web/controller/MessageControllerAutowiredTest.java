package org.sagebionetworks.repo.web.controller;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Headers;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
public class MessageControllerAutowiredTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private MessageService messageService;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private MessageManager messageManager;
	
	private String fileHandleId;
	
	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = false;
	private static final long LIMIT = 100;
	private static final long OFFSET = 0;

	private UserInfo adminUserInfo;
	private Long alice;
	private Long bob;
	private Long eve;
	
	private String aliceId;
	private String bobId;
	private String eveId;
	private Set<String> toAlice;
	private Set<String> toBob;
	private Set<String> toEve;
	
	private String aliceEmail;
	private String bobUsername;
	
	private List<String> cleanup;
	
	@SuppressWarnings("serial")
	private static List<MessageStatusType> inboxFilter = new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}};

	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		cleanup = new ArrayList<String>();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		// Need 3 users
		NewUser user = new NewUser(); 
		aliceEmail = UUID.randomUUID().toString() + "@test.com";
		user.setEmail(aliceEmail);
		user.setUserName(UUID.randomUUID().toString());
		alice = userManager.createUser(user);
		aliceId = "" + alice;
		
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		bobUsername = UUID.randomUUID().toString();
		user.setUserName(bobUsername);
		bob = userManager.createUser(user);
		bobId = "" + bob;
		
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		eve = userManager.createUser(user);
		eveId = "" + eve;
		
		toAlice = new HashSet<String>() {{
			add(aliceId);
		}};
		toBob = new HashSet<String>() {{
			add(bobId);
		}};
		toEve = new HashSet<String>() {{
			add(eveId);
		}};
		
		// We need a file handle to satisfy a foreign key constraint
		String fileContents = "This is text and not an image.";
		S3FileHandle handle = fileHandleManager.createCompressedFileFromString(aliceId, new Date(),
				fileContents);
		fileHandleId = handle.getId();
	}
	
	@After
	public void after() throws Exception {
		try {
			for (String id : cleanup) {
				messageService.deleteMessage(adminUserInfo.getId(), id);
			}
			
			fileHandleManager.deleteFileHandle(adminUserInfo, fileHandleId);
			
			userManager.deletePrincipal(adminUserInfo, Long.parseLong(aliceId));
			userManager.deletePrincipal(adminUserInfo, Long.parseLong(bobId));
			userManager.deletePrincipal(adminUserInfo, Long.parseLong(eveId));
		} catch (Exception e) {
			
		}
	}
	
	/**
	 * Fills in a MessageToUser object with the minimal number of fields
	 */
	private MessageToUser getMessageDTO(Set<String> recipients, String inReplyTo) {
		MessageToUser message = new MessageToUser();
		message.setFileHandleId(fileHandleId);
		message.setRecipients(recipients);
		message.setInReplyTo(inReplyTo);
		return message;
	}
	
	@Test
	public void testCreateMessage() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = servletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		assertNotNull(messageToBob.getId());
		assertEquals(aliceId, messageToBob.getCreatedBy());
		assertNotNull(messageToBob.getCreatedOn());
		assertNotNull(messageToBob.getInReplyToRoot()); 
	}
	
	@Test
	public void testCreateMessage_MissingField() throws Exception {
		MessageToUser aliceToBob = getMessageDTO(toBob, null);
		aliceToBob.setFileHandleId(null);
		try {
			aliceToBob = servletTestHelper.sendMessage(alice, aliceToBob);
			cleanup.add(aliceToBob.getId());
			fail();
		} catch (IllegalArgumentException e) { }
	}
	
	@Test
	public void testGetBoxes() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = servletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		// Process the outgoing message (emulates a worker)
		messageManager.processMessage(messageToBob.getId(), null);
		
		PaginatedResults<MessageToUser> outboxOfAlice = servletTestHelper.getOutbox(alice, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, outboxOfAlice.getTotalNumberOfResults());
		assertEquals(1, outboxOfAlice.getResults().size());
		assertEquals(messageToBob, outboxOfAlice.getResults().get(0));
		
		PaginatedResults<MessageBundle> inboxOfBob = servletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
		
		// This sort of badly formed list should also work
		List<MessageStatusType> weirderFilter = new ArrayList<MessageStatusType>(inboxFilter);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.ARCHIVED);
		
		inboxOfBob = servletTestHelper.getInbox(bob, weirderFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
	}
	
	@Test
	public void testGetMessage() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = servletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		MessageToUser message = servletTestHelper.getMessage(alice, messageToBob.getId());
		assertEquals(messageToBob, message);
		
		message = servletTestHelper.getMessage(bob, messageToBob.getId());
		assertEquals(messageToBob, message);
		
		// No eavesdropping
		try {
			servletTestHelper.getMessage(eve, messageToBob.getId());
			fail();
		} catch (UnauthorizedException e) { }
		
		MessageRecipientSet intercept = new MessageRecipientSet();
		intercept.setRecipients(toEve);
		try {
			servletTestHelper.forwardMessage(eve, messageToBob.getId(), intercept);
			fail();
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testForwardMessage() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = servletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		// Bob forwards the message back to Alice
		MessageRecipientSet bouncy = new MessageRecipientSet();
		bouncy.setRecipients(toAlice);
		MessageToUser messageToAlice = servletTestHelper.forwardMessage(bob, messageToBob.getId(), bouncy);
		cleanup.add(messageToAlice.getId());
		
		// Process the outgoing message (emulates a worker)
		messageManager.processMessage(messageToAlice.getId(), null);
		
		PaginatedResults<MessageBundle> inboxOfAlice = servletTestHelper.getInbox(alice, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfAlice.getTotalNumberOfResults());
		assertEquals(1, inboxOfAlice.getResults().size());
		assertEquals(messageToAlice, inboxOfAlice.getResults().get(0).getMessage());
	}
	
	@Test
	public void testGetConversation() throws Exception {
		MessageToUser aliceToBob = servletTestHelper.sendMessage(alice, getMessageDTO(toBob, null));
		cleanup.add(aliceToBob.getId());
		MessageToUser bobToAlice = servletTestHelper.sendMessage(alice, getMessageDTO(toAlice, aliceToBob.getId()));
		cleanup.add(bobToAlice.getId());
		
		PaginatedResults<MessageToUser> conversation_alice = servletTestHelper.getConversation(alice, aliceToBob.getId(), SORT_ORDER,
				DESCENDING, LIMIT, OFFSET);
		assertEquals(2L, conversation_alice.getTotalNumberOfResults());
		assertEquals(2, conversation_alice.getResults().size());
		assertEquals(aliceToBob, conversation_alice.getResults().get(0));
		assertEquals(bobToAlice, conversation_alice.getResults().get(1));
		
		PaginatedResults<MessageToUser> conversation_bob = servletTestHelper.getConversation(alice, bobToAlice.getId(), SORT_ORDER,
				DESCENDING, LIMIT, OFFSET);
		assertEquals(conversation_alice, conversation_bob);

		// Can't see anything :P
		PaginatedResults<MessageToUser> conversation_eve = servletTestHelper.getConversation(eve, aliceToBob.getId(), SORT_ORDER, DESCENDING,
				LIMIT, OFFSET);
		assertEquals(0L, conversation_eve.getTotalNumberOfResults());
		assertEquals(0, conversation_eve.getResults().size());
	}
	
	@Test
	public void testUpdateStatus() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = servletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		// Process the outgoing message (emulates a worker)
		messageManager.processMessage(messageToBob.getId(), null);
		
		PaginatedResults<MessageBundle> inboxOfBob = servletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
		
		MessageStatus status = new MessageStatus();
		status.setMessageId(messageToBob.getId());
		status.setRecipientId(bobId);
		status.setStatus(MessageStatusType.ARCHIVED);
		servletTestHelper.updateMessageStatus(bob, status);
		
		inboxOfBob = servletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(0L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(0, inboxOfBob.getResults().size());
		
		// Eve can't interfere (does nothing)
		status.setStatus(MessageStatusType.UNREAD);
		try {
			servletTestHelper.updateMessageStatus(eve, status);
			fail();
		} catch (UnauthorizedException e) { }
		
		inboxOfBob = servletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(0L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(0, inboxOfBob.getResults().size());
	}
	
	@Test
	public void testCloudInMessage() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		envelope.setFrom(aliceEmail);
		envelope.setRecipients(Collections.singletonList(bobUsername+"@synapse.org"));
		message.setEnvelope(envelope);
		Headers headers = new Headers();
		headers.setSubject("subject");
		message.setHeaders(headers);
		message.setPlain("this is the message body");
		servletTestHelper.createCloudInMessage(message);
	}
}
