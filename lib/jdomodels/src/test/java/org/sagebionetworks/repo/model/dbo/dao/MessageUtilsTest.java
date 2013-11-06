package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;


public class MessageUtilsTest {
	
	/**
	 * Generates a message that passes the Message utility's validation method, 
	 * but is otherwise filled with junk info 
	 */
	@SuppressWarnings("serial")
	private Message generateSortaValidMessage() {
		Message dto = new Message();
		dto.setMessageId("-1");
		dto.setCreatedBy("-3");
		dto.setRecipientType(ObjectType.PRINCIPAL);
		dto.setRecipients(new HashSet<String>() {{add("-4");}});
		dto.setMessageFileHandleId("-5");
		dto.setCreatedOn(new Date());
		return dto;
	}
	
	@Test
	public void testValidateMessage() throws Exception {
		// The static method gives us a passing message
		DBOMessage dbo = MessageUtils.convertDTO(generateSortaValidMessage());
		MessageUtils.validateDBO(dbo);
		
		// The only optional field of a message is the subject
		dbo.setSubject("I'm not null");
		MessageUtils.validateDBO(dbo);
		
		// Still valid
		dbo.setSubject(null);
		MessageUtils.validateDBO(dbo);
		
		// All of the following should throw different error messages
		Set<String> caughtMessages = new HashSet<String>();
		
		dbo.setCreatedOn(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setFileHandleId(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setRecipients(MessageUtils.zip(new HashSet<String>()));
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		// Except for this one, which is the same message as the previous one
		dbo.setRecipients(null);
		assertFalse(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setRecipientType(ObjectType.FAVORITE.name());
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setRecipientType(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setCreatedBy(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
		
		dbo.setMessageId(null);
		assertTrue(caughtMessages.add(validateMessageCatchIllegalArgument(dbo)));
	}
	
	/**
	 * Helper for the test to validate the validateMessage helper
	 */
	private String validateMessageCatchIllegalArgument(DBOMessage dbo) {
		try {
			MessageUtils.validateDBO(dbo);
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
