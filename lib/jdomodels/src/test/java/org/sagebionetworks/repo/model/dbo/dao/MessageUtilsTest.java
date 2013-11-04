package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.RecipientType;


public class MessageUtilsTest {
	
	/**
	 * Generates a message that passes the Message utility's validation method, 
	 * but is otherwise filled with junk info 
	 */
	@SuppressWarnings("serial")
	private Message generateSortaValidMessage() {
		Message dto = new Message();
		dto.setMessageId("-1");
		dto.setThreadId("-2");
		dto.setCreatedBy("-3");
		dto.setRecipientType(RecipientType.PRINCIPAL);
		dto.setRecipients(new HashSet<String>() {{add("-4");}});
		dto.setMessageFileHandleId("-5");
		dto.setCreatedOn(new Date());
		return dto;
	}
	
	@Test
	public void testValidateMessage() throws Exception {
		// The static method gives us a passing message
		Message dto = generateSortaValidMessage();
		MessageUtils.validateDTO(dto);
		
		// The only optional field of a message is the subject
		dto.setSubject("I'm not null");
		MessageUtils.validateDTO(dto);
		
		// Still valid
		dto.setSubject(null);
		MessageUtils.validateDTO(dto);
		
		// All of the following should throw different error messages
		Set<String> caughtMessages = new HashSet<String>();
		
		dto.setCreatedOn(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setMessageFileHandleId(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setRecipients(new HashSet<String>());
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		// Except for this one, which is the same message as the previous one
		dto.setRecipients(null);
		assertFalse(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setRecipientType(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setCreatedBy(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setThreadId(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
		
		dto.setMessageId(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dto)));
	}
	
	/**
	 * Helper for the test to validate the validateMessage helper
	 */
	private String validateMessageCatchIllegalArgument(Message dto) {
		try {
			MessageUtils.validateDTO(dto);
			fail();
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
		throw new RuntimeException("Impossible to reach this point in a test");
	}
	
	@Test
	public void testMessageConversion() throws Exception {
		Message dto = generateSortaValidMessage();
		DBOMessage dbo = MessageUtils.convertDTO(dto);
		Message dto2 = MessageUtils.convertDBO(dbo);
		
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testStatusConversion() throws Exception {
		MessageStatus dto = new MessageStatus();
		dto.setMessageId("-1");
		dto.setRecipientId("-1");
		dto.setStatus(MessageStatusType.UNREAD);
		
		DBOMessageStatus dbo = MessageUtils.convertDTO(dto);
		MessageStatus dto2 = MessageUtils.convertDBO(dbo);
		
		assertEquals(dto, dto2);
	}
	
	@Test
	public void testZipUnzip() throws Exception {
		Set<String> original = new HashSet<String>();
		original.add("1");
		original.add("1");
		original.add("2");
		original.add("34");
		original.add("567");
		original.add("8901");
		original.add("23456789");
		original.add(Long.toString(Long.MIN_VALUE));
		original.add(Long.toString(Long.MAX_VALUE));
		
		Set<String> processed = MessageUtils.unzip(MessageUtils.zip(original));
		assertTrue(original.containsAll(processed));
		assertTrue(processed.containsAll(original));
	}
}
