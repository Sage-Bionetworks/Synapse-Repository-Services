package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.message.MessageToUser;

public class MessageUtils {

	/**
	 * Copies message information from three DBOs into one DTO
	 * The DBOs should be complimentary (same message ID)
	 * See {@link #copyDBOToDTO(DBOMessageContent, MessageToUser)} and {@link #copyDBOToDTO(DBOMessageToUser, MessageToUser)}
	 */
	public static void copyDBOToDTO(DBOMessageContent content, DBOMessageToUser info, List<DBOMessageRecipient> recipients, MessageToUser bundle) {
		if (recipients.size() > 0 && content.getMessageId() != recipients.get(0).getMessageId()) {
			throw new IllegalArgumentException("Message content and recipients should be belong to the same message");
		}
		copyDBOToDTO(content, info, bundle);
		copyDBOToDTO(recipients, bundle);
	}
	
	/**
	 * Copies message information from two DBOs into one DTO
	 * The DBOs should be complimentary (same message ID)
	 * See {@link #copyDBOToDTO(DBOMessageContent, MessageToUser)} and {@link #copyDBOToDTO(DBOMessageToUser, MessageToUser)}
	 */
	public static void copyDBOToDTO(DBOMessageContent content, DBOMessageToUser info, MessageToUser bundle) {
		if (content.getMessageId() != info.getMessageId()) {
			throw new IllegalArgumentException("Message content and information should belong to the same message");
		}
		copyDBOToDTO(content, bundle);
		copyDBOToDTO(info, bundle);
	}
	
	/**
	 * Copies message information
	 * Note: DBOMessageContent contains a subset of the fields of MessageToUser
	 * Note: Etag information is not transfered
	 */
	public static void copyDBOToDTO(DBOMessageContent content, MessageToUser bundle) {
		bundle.setId(toString(content.getMessageId()));
		bundle.setCreatedBy(toString(content.getCreatedBy()));
		bundle.setFileHandleId(toString(content.getFileHandleId()));
		if (content.getCreatedOn() != null) {
			bundle.setCreatedOn(new Date(content.getCreatedOn()));
		}
		bundle.setInReplyTo(toString(content.getInReplyTo()));
	}
	
	/**
	 * Copies message information 
	 * Note: DBOMessageToUser contains a subset of the fields of MessageToUser
	 */
	public static void copyDBOToDTO(DBOMessageToUser info, MessageToUser bundle) {
		bundle.setId(toString(info.getMessageId()));
		bundle.setSubject(info.getSubject());
		bundle.setInReplyToRoot(toString(info.getRootMessageId()));
	}
	
	/**
	 * Copies message information 
	 * Note: DBOMessageRecipient contains a subset of the fields of MessageToUser
	 */
	public static void copyDBOToDTO(List<DBOMessageRecipient> recipients, MessageToUser bundle) {
		if (recipients.size() > 0) {
			bundle.setId(toString(recipients.get(0).getMessageId()));
		}
		bundle.setRecipients(new HashSet<String>());
		for (DBOMessageRecipient recipient : recipients) {
			if (recipient.getMessageId() != recipients.get(0).getMessageId()) {
				throw new IllegalArgumentException("Message recipients should be belong to the same message");
			}
			bundle.getRecipients().add(toString(recipient.getRecipientId()));
		}
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
	 * Copies message information from one DTO into three DBOs
	 * Note: some information, like message ID, will be duplicated
	 * See {@link #copyDTOToDBO(MessageToUser, DBOMessageContent)}, 
	 *     {@link #copyDTOToDBO(MessageToUser, DBOMessageToUser)}, and 
	 *     {@link #copyDTOToDBO(MessageToUser, List<DBOMessageRecipient>)}
	 */
	public static void copyDTOtoDBO(MessageToUser dto, DBOMessageContent content, DBOMessageToUser info, List<DBOMessageRecipient> recipients) {
		copyDTOToDBO(dto, content);
		copyDTOToDBO(dto, info);
		copyDTOToDBO(dto, recipients);
	}
	
	/**
	 * Copies message information
	 * Note: DBOMessageContent contains a subset of the fields of MessageToUser
	 * Note: Etag information is not transfered
	 */
	public static void copyDTOToDBO(MessageToUser dto, DBOMessageContent content) {
		content.setMessageId(parseLong(dto.getId()));
		content.setCreatedBy(parseLong(dto.getCreatedBy()));
		content.setFileHandleId(parseLong(dto.getFileHandleId()));
		if (dto.getCreatedOn() != null) {
			content.setCreatedOn(dto.getCreatedOn().getTime());
		}
		content.setInReplyTo(parseLong(dto.getInReplyTo()));
	}
	
	/**
	 * Copies message information
	 * Note: DBOMessageToUser contains a subset of the fields of MessageToUser
	 */
	public static void copyDTOToDBO(MessageToUser dto, DBOMessageToUser info) {
		info.setMessageId(parseLong(dto.getId()));
		info.setSubject(dto.getSubject());
		info.setRootMessageId(parseLong(dto.getInReplyToRoot()));
	}

	/**
	 * Copies message information
	 * Note: DBOMessageRecipient contains a subset of the fields of MessageToUser
	 */
	public static void copyDTOToDBO(MessageToUser dto, List<DBOMessageRecipient> recipients) {
		for (String recipient : dto.getRecipients()) {
			DBOMessageRecipient dbo = new DBOMessageRecipient();
			dbo.setMessageId(parseLong(dto.getId()));
			dbo.setRecipientId(parseLong(recipient));
			recipients.add(dbo);
		}
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
