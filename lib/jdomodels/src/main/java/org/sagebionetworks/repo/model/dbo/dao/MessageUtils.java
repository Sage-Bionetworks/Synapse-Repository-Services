package org.sagebionetworks.repo.model.dbo.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;

public class MessageUtils {
	
	public static List<Message> convertDBOs(List<DBOMessageContent> dbos) {
		List<Message> dtos = new ArrayList<Message>();
		for (DBOMessageContent dbo : dbos) {
			dtos.add(convertDBO(dbo));
		}
		return dtos;
	}
	
	public static Message convertDBO(DBOMessageContent dbo) {
		Message dto = new Message();
		dto.setMessageId(toString(dbo.getMessageId()));
		dto.setCreatedBy(toString(dbo.getCreatedBy()));
		dto.setRecipientType(ObjectType.valueOf(dbo.getRecipientType()));
		try {
			dto.setRecipients(unzip(dbo.getRecipients()));
		} catch (IOException e) {
			throw new DatastoreException("Could not unpack the list of intended recipients", e);
		}
		dto.setMessageFileHandleId(toString(dbo.getFileHandleId()));
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setSubject(dbo.getSubject());
		dto.setReplyTo(toString(dbo.getReplyTo()));
		return dto;
	}
	
	/**
	 * Checks for all required fields of the DBO
	 */
	public static void validateDBO(DBOMessageContent dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Message ID must be specified");
		}
		if (dbo.getCreatedBy() == null) {
			throw new IllegalArgumentException("Sender's ID must be specified");
		}
		if (dbo.getRecipientType() == null) {
			throw new IllegalArgumentException("Recipient type must be specified");
		}
		switch (ObjectType.valueOf(dbo.getRecipientType())) {
		case PRINCIPAL:
		case ENTITY:
			break;
		default:
			throw new IllegalArgumentException("Recipient type must be either PRINCIPAL or ENTITY");
		}
		try {
			if (dbo.getRecipients() == null || unzip(dbo.getRecipients()).size() <= 0) {
				throw new IllegalArgumentException("Recipients must be specified");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (dbo.getFileHandleId() == null) {
			throw new IllegalArgumentException("Message body's file handle must be specified");
		}
		if (dbo.getCreatedOn() == null) {
			throw new IllegalArgumentException("Time of sending must be specified");
		}
	}
	
	public static DBOMessageContent convertDTO(Message dto) {
		DBOMessageContent dbo = new DBOMessageContent();
		dbo.setMessageId(parseLong(dto.getMessageId()));
		dbo.setCreatedBy(parseLong(dto.getCreatedBy()));
		if (dto.getRecipientType() != null) {
			dbo.setRecipientType(dto.getRecipientType().name());
		}
		try {
			dbo.setRecipients(zip(dto.getRecipients()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dbo.setFileHandleId(parseLong(dto.getMessageFileHandleId()));
		if (dto.getCreatedOn() != null) {
			dbo.setCreatedOn(dto.getCreatedOn().getTime());
		}
		dbo.setSubject(dto.getSubject());
		dbo.setReplyTo(parseLong(dto.getReplyTo()));
		return dbo;
	}
	
	/**
	 * Gzips the given list. 
	 * @param longs Each element must be parsable as a long
	 * @return A gzipped byte array of long ints 
	 */
	protected static byte[] zip(Set<String> longs) throws IOException {
		if (longs == null) {
			return null;
		}
		
		// Convert the Strings into Longs into Bytes
		ByteBuffer converter = ByteBuffer.allocate(Long.SIZE / 8 * longs.size());
		for (String num : longs) {
			converter.putLong(KeyFactory.stringToKey(num));
		}
		
		// Zip up the bytes
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipped = new GZIPOutputStream(out);
		zipped.write(converter.array());
		zipped.flush();
		zipped.close();
		return out.toByteArray();
	}
	
	/**
	 * Un-gzips the given array.  
	 * @param zippedLongs Gzipped array of long ints
	 * @return A list of longs in base 10 string form
	 */
	protected static Set<String> unzip(byte[] zippedLongs) throws IOException {
		if (zippedLongs == null) {
			return null;
		}
		
		// Unzip the bytes
		ByteArrayInputStream in = new ByteArrayInputStream(zippedLongs);
		GZIPInputStream unzip = new GZIPInputStream(in);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (unzip.available() > 0) {
			out.write(unzip.read());
		}
		
		// Convert to bytes
		ByteBuffer converter = ByteBuffer.wrap(out.toByteArray());
		LongBuffer converted = converter.asLongBuffer();
		Set<String> verbose = new HashSet<String>();
		while (converted.hasRemaining()) {
			verbose.add(Long.toString(converted.get()));
		}
		return verbose;
	}
	
	public static MessageStatus convertDBO(DBOMessageStatus dbo) {
		MessageStatus dto = new MessageStatus();
		dto.setMessageId(toString(dbo.getMessageId()));
		dto.setRecipientId(toString(dbo.getRecipientId()));
		dto.setStatus(MessageStatusType.valueOf(dbo.getStatus()));
		return dto;
	}
	
	public static DBOMessageStatus convertDTO(MessageStatus dto) {
		DBOMessageStatus dbo = new DBOMessageStatus();
		dbo.setMessageId(parseLong(dto.getMessageId()));
		dbo.setRecipientId(parseLong(dto.getRecipientId()));
		dbo.setStatus(dto.getStatus());
		return dbo;
	}
	
	/**
	 * Null tolerant call to input.toString()
	 */
	private static String toString(Long input) {
		if (input == null) {
			return null;
		}
		return input.toString();
	}
	
	/**
	 * Null-tolerant call to Long.parseLong(...)
	 */
	private static Long parseLong(String input) {
		if (input == null) {
			return null;	
		}
		return Long.parseLong(input);
	}
}
