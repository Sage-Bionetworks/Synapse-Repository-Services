package org.sagebionetworks.repo.web.controller;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Simplistic test to see if things are wired up correctly
 * All messages are retrieved oldest first
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MessageControllerAutowiredTest {
	
	@Autowired
	private ServletTestHelper testHelper;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private MessageService messageService;
	
	@Autowired
	private FileHandleDao fileDAO;
	private String fileHandleId;
	
	private static final MessageSortBy SORT_ORDER = MessageSortBy.SEND_DATE;
	private static final boolean DESCENDING = false;
	private static final long LIMIT = 100;
	private static final long OFFSET = 0;

	private final String alice = AuthorizationConstants.TEST_USER_NAME;
	private final String bob = StackConfiguration.getIntegrationTestUserOneName();
	private final String eve = StackConfiguration.getIntegrationTestUserTwoName();
	
	private String aliceId;
	private String bobId;
	private String eveId;
	private Set<String> toAlice;
	private Set<String> toBob;
	private Set<String> toEve;
	
	private List<String> cleanup;
	
	@SuppressWarnings("serial")
	private static List<MessageStatusType> inboxFilter = new ArrayList<MessageStatusType>() {{add(MessageStatusType.UNREAD);}};

	@SuppressWarnings("serial")
	@Before
	public void before() throws Exception {
		cleanup = new ArrayList<String>();
		testHelper.setUp();
		
		aliceId = userManager.getUserInfo(alice).getIndividualGroup().getId();
		bobId = userManager.getUserInfo(bob).getIndividualGroup().getId();
		eveId = userManager.getUserInfo(eve).getIndividualGroup().getId();
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
		// But it doesn't need to point to an actual file
		// Also, it doesn't matter who the handle is tied to
		S3FileHandle handle = new S3FileHandle();
		handle.setBucketName("bucketName");
		handle.setKey("key");
		handle.setContentType("content type");
		handle.setContentSize(123l);
		handle.setContentMd5("md5");
		handle.setCreatedBy(aliceId);
		handle.setFileName("foobar.txt");
		handle = fileDAO.createFile(handle);
		fileHandleId = handle.getId();
	}
	
	@After
	public void after() throws Exception {
		for (String id : cleanup) {
			messageService.deleteMessage(AuthorizationConstants.ADMIN_USER_NAME, id);
		}
		fileDAO.delete(fileHandleId);
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
		messageToBob = ServletTestHelper.sendMessage(alice, messageToBob);
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
			aliceToBob = ServletTestHelper.sendMessage(alice, aliceToBob);
			cleanup.add(aliceToBob.getId());
			fail();
		} catch (IllegalArgumentException e) { }
	}
	
	@Test
	public void testGetBoxes() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = ServletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		PaginatedResults<MessageToUser> outboxOfAlice = ServletTestHelper.getOutbox(alice, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, outboxOfAlice.getTotalNumberOfResults());
		assertEquals(1, outboxOfAlice.getResults().size());
		assertEquals(messageToBob, outboxOfAlice.getResults().get(0));
		
		PaginatedResults<MessageBundle> inboxOfBob = ServletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
		
		// This sort of badly formed list should also work
		List<MessageStatusType> weirderFilter = new ArrayList<MessageStatusType>(inboxFilter);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.READ);
		weirderFilter.add(MessageStatusType.ARCHIVED);
		
		inboxOfBob = ServletTestHelper.getInbox(bob, weirderFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
	}
	
	@Test
	public void testGetMessage() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = ServletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		MessageToUser message = ServletTestHelper.getMessage(alice, messageToBob.getId());
		assertEquals(messageToBob, message);
		
		message = ServletTestHelper.getMessage(bob, messageToBob.getId());
		assertEquals(messageToBob, message);
		
		// No eavesdropping
		try {
			ServletTestHelper.getMessage(eve, messageToBob.getId());
			fail();
		} catch (UnauthorizedException e) { }
		
		MessageRecipientSet intercept = new MessageRecipientSet();
		intercept.setRecipients(toEve);
		try {
			ServletTestHelper.forwardMessage(eve, messageToBob.getId(), intercept);
			fail();
		} catch (UnauthorizedException e) { }
	}
	
	@Test
	public void testForwardMessage() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = ServletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		// Bob forwards the message back to Alice
		MessageRecipientSet bouncy = new MessageRecipientSet();
		bouncy.setRecipients(toAlice);
		MessageToUser messageToAlice = ServletTestHelper.forwardMessage(bob, messageToBob.getId(), bouncy);
		cleanup.add(messageToAlice.getId());
		
		PaginatedResults<MessageBundle> inboxOfAlice = ServletTestHelper.getInbox(alice, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfAlice.getTotalNumberOfResults());
		assertEquals(1, inboxOfAlice.getResults().size());
		assertEquals(messageToAlice, inboxOfAlice.getResults().get(0).getMessage());
	}
	
	@Test
	public void testGetConversation() throws Exception {
		MessageToUser aliceToBob = ServletTestHelper.sendMessage(alice, getMessageDTO(toBob, null));
		cleanup.add(aliceToBob.getId());
		MessageToUser bobToAlice = ServletTestHelper.sendMessage(alice, getMessageDTO(toAlice, aliceToBob.getId()));
		cleanup.add(bobToAlice.getId());
		
		PaginatedResults<MessageToUser> conversation_alice = ServletTestHelper.getConversation(alice, aliceToBob.getId(), SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(2L, conversation_alice.getTotalNumberOfResults());
		assertEquals(2, conversation_alice.getResults().size());
		assertEquals(aliceToBob, conversation_alice.getResults().get(0));
		assertEquals(bobToAlice, conversation_alice.getResults().get(1));
		
		PaginatedResults<MessageToUser> conversation_bob = ServletTestHelper.getConversation(alice, bobToAlice.getId(), SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(conversation_alice, conversation_bob);

		// Can't see anything :P
		PaginatedResults<MessageToUser> conversation_eve = ServletTestHelper.getConversation(eve, aliceToBob.getId(), SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(0L, conversation_eve.getTotalNumberOfResults());
		assertEquals(0, conversation_eve.getResults().size());
	}
	
	@Test
	public void testUpdateStatus() throws Exception {
		MessageToUser messageToBob = getMessageDTO(toBob, null);
		messageToBob = ServletTestHelper.sendMessage(alice, messageToBob);
		cleanup.add(messageToBob.getId());
		
		PaginatedResults<MessageBundle> inboxOfBob = ServletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(1L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(1, inboxOfBob.getResults().size());
		assertEquals(messageToBob, inboxOfBob.getResults().get(0).getMessage());
		
		MessageStatus status = new MessageStatus();
		status.setMessageId(messageToBob.getId());
		status.setRecipientId(bobId);
		status.setStatus(MessageStatusType.ARCHIVED);
		ServletTestHelper.updateMessageStatus(bob, status);
		
		inboxOfBob = ServletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(0L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(0, inboxOfBob.getResults().size());
		
		// Eve can't interfere (does nothing)
		status.setStatus(MessageStatusType.UNREAD);
		try {
			ServletTestHelper.updateMessageStatus(eve, status);
			fail();
		} catch (UnauthorizedException e) { }
		
		inboxOfBob = ServletTestHelper.getInbox(bob, inboxFilter, SORT_ORDER, DESCENDING, LIMIT, OFFSET);
		assertEquals(0L, inboxOfBob.getTotalNumberOfResults());
		assertEquals(0, inboxOfBob.getResults().size());
	}
}
