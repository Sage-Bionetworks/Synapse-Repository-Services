package org.sagebionetworks.repo.model.dbo.dao;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOComment;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageRecipient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageStatus;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.message.Comment;
import org.sagebionetworks.repo.model.message.MessageContent;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.util.ValidateArgument;

public class MessageUtils {
	public static final int BLOB_MAX_SIZE = 65535;

	/**
	 * Copies message information from three DBOs into one DTO
	 * The DBOs should be complimentary (same message ID)
	 * See {@link #copyDBOToDTO(DBOMessageContent, DBOMessageToUser, MessageToUser)} and {@link #copyDBOToDTO(List, MessageToUser)}
	 */
	public static void copyDBOToDTO(DBOMessageContent content, DBOMessageToUser info, List<DBOMessageRecipient> recipients, MessageToUser bundle) {
		if (recipients.size() > 0 && !content.getMessageId().equals(recipients.get(0).getMessageId())) {
			throw new IllegalArgumentException("Message content and recipients should be belong to the same message");
		}
		copyDBOToDTO(content, info, bundle);
		copyDBOToDTO(recipients, bundle);
	}
	
	/**
	 * Copies message information from two DBOs into one DTO
	 * The DBOs should be complimentary (same message ID)
	 * See {@link #copyDBOToDTO(DBOMessageContent, MessageContent)} and {@link #copyDBOToDTO(DBOMessageContent, MessageContent}
	 */
	public static void copyDBOToDTO(DBOMessageContent content, DBOMessageToUser info, MessageToUser bundle) {
		if (!content.getMessageId().equals(info.getMessageId())) {
			throw new IllegalArgumentException("Message content and information should belong to the same message");
		}
		copyDBOToDTO(content, bundle);
		copyDBOToDTO(info, bundle);
	}
	
	/**
	 * Copies comment information from two DBOs into one DTO
	 * The DBOs should be complimentary (same message ID)
	 * See {@link #copyDBOToDTO(DBOMessageContent, MessageContent)} and {@link #copyDBOToDTO(DBOComment, Comment)}
	 */
	public static void copyDBOToDTO(DBOMessageContent content, DBOComment info, Comment bundle) {
		if (!content.getMessageId().equals(info.getMessageId())) {
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
	public static void copyDBOToDTO(DBOMessageContent content, MessageContent bundle) {
		bundle.setId(toString(content.getMessageId()));
		bundle.setCreatedBy(toString(content.getCreatedBy()));
		bundle.setFileHandleId(toString(content.getFileHandleId()));
		if (content.getCreatedOn() != null) {
			bundle.setCreatedOn(new Date(content.getCreatedOn()));
		}
	}
	
	private static final String SUBJECT_CHARACTER_SET = "UTF-8";
	/**
	 * Copies message information 
	 * Note: DBOMessageToUser contains a subset of the fields of MessageToUser
	 */
	public static void copyDBOToDTO(DBOMessageToUser info, MessageToUser bundle) {
		bundle.setId(toString(info.getMessageId()));
		bundle.setInReplyToRoot(toString(info.getRootMessageId()));
		bundle.setInReplyTo(toString(info.getInReplyTo()));
		if (info.getSubjectBytes()==null) {
			bundle.setSubject(null);
		} else {
			try {
				bundle.setSubject(new String(info.getSubjectBytes(), SUBJECT_CHARACTER_SET));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		bundle.setNotificationUnsubscribeEndpoint(info.getNotificationsEndpoint());
		bundle.setUserProfileSettingEndpoint(info.getProfileSettingEndpoint());
		bundle.setWithUnsubscribeLink(info.getWithUnsubscribeLink());
		bundle.setWithProfileSettingLink(info.getWithProfileSettingLink());
		bundle.setIsNotificationMessage(info.getIsNotificationMessage());
		try {
			if (info.getBytesTo() != null) {
				bundle.setTo(new String(info.getBytesTo(), "UTF-8"));
			} else {
				bundle.setTo(null);
			}
			if (info.getBytesCc() != null) {
				bundle.setCc(new String(info.getBytesCc(), "UTF-8"));
			} else {
				bundle.setCc(null);
			}
			if (info.getBytesBcc() != null) {
				bundle.setBcc(new String(info.getBytesBcc(), "UTF-8"));
			} else {
				bundle.setBcc(null);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Copies comment information 
	 * Note: DBOComment contains a subset of the fields of Comment
	 */
	public static void copyDBOToDTO(DBOComment info, Comment bundle) {
		bundle.setId(toString(info.getMessageId()));
		bundle.setTargetId(toString(info.getObjectId()));
		if (info.getObjectType() != null) {
			bundle.setTargetType(ObjectType.valueOf(info.getObjectType()));
		}
	}
	
	/**
	 * Copies message information 
	 * All recipients must belong to the same message
	 * Note: DBOMessageRecipient contains a subset of the fields of MessageToUser
	 */
	public static void copyDBOToDTO(List<DBOMessageRecipient> recipients, MessageToUser bundle) {
		if (recipients.size() > 0) {
			bundle.setId(toString(recipients.get(0).getMessageId()));
		}
		bundle.setRecipients(new HashSet<String>());
		for (DBOMessageRecipient recipient : recipients) {
			if (!recipient.getMessageId().equals(recipients.get(0).getMessageId())) {
				throw new IllegalArgumentException("Message recipients should be belong to the same message");
			}
			bundle.getRecipients().add(toString(recipient.getRecipientId()));
		}
	}
	
	/**
	 * Copies message information from recipients into the appropriate and matching MessageToUser
	 * Note: DBOMessageRecipient contains a subset of the fields of MessageToUser
	 */
	public static void copyDBOToDTO(List<DBOMessageRecipient> recipients, List<MessageToUser> bundles) {
		// Map the message list to IDs for faster lookup
		Map<Long, MessageToUser> buckets = new HashMap<Long, MessageToUser>();
		for (MessageToUser message : bundles) {
			if (message.getId() == null) {
				throw new IllegalArgumentException("Message must have an ID");
			}
			buckets.put(Long.parseLong(message.getId()), message);
			
			// Also initialize the recipient set
			message.setRecipients(new HashSet<String>());
		}
		
		// Chuck recipients into buckets
		for (DBOMessageRecipient recipient : recipients) {
			if (!buckets.containsKey(recipient.getMessageId())) {
				throw new IllegalArgumentException("No matching MessageToUser (" + recipient.getMessageId() + ") found for recipient (" + recipient.getRecipientId() + ")");
			}
			buckets.get(recipient.getMessageId()).getRecipients().add(toString(recipient.getRecipientId()));
		}
	}
	
	/**
	 * Copies message status info 
	 */
	public static MessageStatus convertDBO(DBOMessageStatus dbo) {
		MessageStatus dto = new MessageStatus();
		dto.setMessageId(toString(dbo.getMessageId()));
		dto.setRecipientId(toString(dbo.getRecipientId()));
		dto.setStatus(MessageStatusType.valueOf(dbo.getStatus()));
		return dto;
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
	 * See {@link #copyDTOToDBO(MessageContent, DBOMessageContent)}, 
	 *     {@link #copyDTOToDBO(MessageToUser, DBOMessageToUser)}, and 
	 *     {@link #copyDTOToDBO(MessageToUser, List)}
	 */
	public static void copyDTOtoDBO(MessageToUser dto, DBOMessageContent content, DBOMessageToUser info, List<DBOMessageRecipient> recipients) {
		copyDTOToDBO(dto, content);
		copyDTOToDBO(dto, info);
		copyDTOToDBO(dto, recipients);
	}
	
	/**
	 * Copies comment information from one DTO into two DBOs
	 * Note: some information, like message ID, will be duplicated
	 * See {@link #copyDTOToDBO(MessageContent, DBOMessageContent)} and {@link #copyDTOToDBO(Comment, DBOComment)}
	 */
	public static void copyDTOtoDBO(Comment dto, DBOMessageContent content, DBOComment info) {
		copyDTOToDBO(dto, content);
		copyDTOToDBO(dto, info);
	}
	
	/**
	 * Copies message information
	 * Note: DBOMessageContent contains a subset of the fields of MessageToUser
	 * Note: Etag information is not transfered
	 */
	public static void copyDTOToDBO(MessageContent dto, DBOMessageContent content) {
		content.setMessageId(parseLong(dto.getId()));
		content.setCreatedBy(parseLong(dto.getCreatedBy()));
		content.setFileHandleId(parseLong(dto.getFileHandleId()));
		if (dto.getCreatedOn() != null) {
			content.setCreatedOn(dto.getCreatedOn().getTime());
		}
	}
	
	/**
	 * Copies message information
	 * Note: DBOMessageToUser contains a subset of the fields of MessageToUser
	 */
	public static void copyDTOToDBO(MessageToUser dto, DBOMessageToUser info) {
		info.setMessageId(parseLong(dto.getId()));
		info.setRootMessageId(parseLong(dto.getInReplyToRoot()));
		info.setInReplyTo(parseLong(dto.getInReplyTo()));
		if (dto.getSubject()==null) {
			info.setSubjectBytes(null);
		} else {
			try {
				info.setSubjectBytes(dto.getSubject().getBytes(SUBJECT_CHARACTER_SET));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		info.setNotificationsEndpoint(dto.getNotificationUnsubscribeEndpoint());
		info.setProfileSettingEndpoint(dto.getUserProfileSettingEndpoint());
		if (dto.getWithUnsubscribeLink() == null) {
			info.setWithUnsubscribeLink(false);
		} else {
			info.setWithUnsubscribeLink(dto.getWithUnsubscribeLink());
		}
		if (dto.getWithProfileSettingLink() == null) {
			info.setWithProfileSettingLink(false);
		} else {
			info.setWithProfileSettingLink(dto.getWithProfileSettingLink());
		}
		if (dto.getIsNotificationMessage() == null) {
			info.setIsNotificationMessage(false);
		} else {
			info.setIsNotificationMessage(dto.getIsNotificationMessage());
		}
		// Make sure to set the override to false by default
		if (info.getOverrideNotificationSettings() == null) {
			info.setOverrideNotificationSettings(false);
		}
		try {
			if (dto.getTo() != null) {
				info.setBytesTo(dto.getTo().getBytes("UTF-8"));
			} else {
				info.setBytesTo(null);
			}
			if (dto.getCc() != null) {
				info.setBytesCc(dto.getCc().getBytes("UTF-8"));
			} else {
				info.setBytesCc(null);
			}
			if (dto.getBcc() != null) {
				info.setBytesBcc(dto.getBcc().getBytes("UTF-8"));
			} else {
				info.setBytesBcc(null);
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Copies comment information
	 * Note: DBOComment contains a subset of the fields of Comment
	 */
	public static void copyDTOToDBO(Comment dto, DBOComment info) {
		info.setMessageId(parseLong(dto.getId()));
		info.setObjectId(parseLong(dto.getTargetId()));
		if (dto.getTargetType() != null) {
			info.setObjectType(dto.getTargetType().name());
		}
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
	 * Copies message status info 
	 */
	public static DBOMessageStatus convertDTO(MessageStatus dto) {
		DBOMessageStatus dbo = new DBOMessageStatus();
		dbo.setMessageId(parseLong(dto.getMessageId()));
		dbo.setRecipientId(parseLong(dto.getRecipientId()));
		dbo.setStatus(dto.getStatus());
		return dbo;
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
	
	/**
	 * Checks for all required fields
	 */
	public static void validateDBO(DBOMessageContent dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Message content must have an ID");
		}
		if (dbo.getCreatedBy() == null) {
			throw new IllegalArgumentException("Message content must have a creator");
		}
		if (dbo.getFileHandleId() == null) {
			throw new IllegalArgumentException("Message content must have a file handle");
		}
		if (dbo.getEtag() == null) {
			throw new IllegalArgumentException("Message content must have an etag");
		}
		if (dbo.getCreatedOn() == null) {
			throw new IllegalArgumentException("Message content must have a creation time");
		}
	}
	
	/**
	 * Checks for all required fields
	 */
	public static void validateDBO(DBOMessageToUser dbo) {
		ValidateArgument.required(dbo.getMessageId(), "Message info must have an ID.");
		ValidateArgument.required(dbo.getRootMessageId(), "Message info must point to a root message.");
		ValidateArgument.required(dbo.getSent(), "Message info must have a status value.");
		ValidateArgument.requirement(dbo.getBytesTo() == null || dbo.getBytesTo().length <= BLOB_MAX_SIZE,
				"To field must be "+ BLOB_MAX_SIZE +" bytes long or shorter.");
		ValidateArgument.requirement(dbo.getBytesCc() == null || dbo.getBytesCc().length <= BLOB_MAX_SIZE,
				"CC field must be "+ BLOB_MAX_SIZE +" bytes long or shorter.");
		ValidateArgument.requirement(dbo.getBytesBcc() == null || dbo.getBytesBcc().length <= BLOB_MAX_SIZE,
				"BCC field must be "+ BLOB_MAX_SIZE +" bytes long or shorter.");
	}
	
	/**
	 * Checks for all required fields
	 */
	public static void validateDBO(DBOComment dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Comment info must have an ID");
		}
		if (dbo.getObjectId() == null) {
			throw new IllegalArgumentException("Comment info must point to an object");
		}
		if (dbo.getObjectType() == null) {
			throw new IllegalArgumentException("Comment info must point to an object type");
		}
	}
	
	/**
	 * Checks for all required fields
	 */
	public static void validateDBO(DBOMessageRecipient dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Message recipient must have a message ID");
		}
		if (dbo.getRecipientId() == null) {
			throw new IllegalArgumentException("Message recipient must have an user ID");
		}
	}

	/**
	 * Checks for all required fields
	 */
	public static void validateDBO(DBOMessageStatus dbo) {
		if (dbo.getMessageId() == null) {
			throw new IllegalArgumentException("Message status must have a message ID");
		}
		if (dbo.getRecipientId() == null) {
			throw new IllegalArgumentException("Message status must have a recipient ID");
		}
		if (dbo.getStatus() == null) {
			throw new IllegalArgumentException("Message status must have a status");
		}
	}
}
