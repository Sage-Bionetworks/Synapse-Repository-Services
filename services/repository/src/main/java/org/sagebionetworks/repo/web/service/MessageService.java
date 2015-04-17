package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageService {

	public MessageToUser create(Long userId, MessageToUser toCreate)
			throws NotFoundException;

	public PaginatedResults<MessageBundle> getInbox(Long userId,
			List<MessageStatusType> inclusionFilter, MessageSortBy sortBy,
			boolean descending, long limit, long offset, String urlPath)
			throws NotFoundException;

	public PaginatedResults<MessageToUser> getOutbox(Long userId,
			MessageSortBy sortBy, boolean descending, long limit, long offset,
			String urlPath) throws NotFoundException;

	public MessageToUser getMessage(Long userId, String messageId)
			throws NotFoundException;

	public MessageToUser forwardMessage(Long userId, String messageId,
			MessageRecipientSet recipients) throws NotFoundException;

	public PaginatedResults<MessageToUser> getConversation(Long userId,
			String messageId, MessageSortBy sortBy, boolean descending,
			long limit, long offset, String urlPath)
			throws NotFoundException;

	public void updateMessageStatus(Long userId, MessageStatus status)
			throws NotFoundException;
	
	public void deleteMessage(Long userId, String messageId)
			throws NotFoundException;

	public String getMessageFileRedirectURL(Long userId, String messageId) throws NotFoundException;

	public MessageToUser createMessageToEntityOwner(Long userId, String entityId,
			MessageToUser toCreate) throws NotFoundException, ACLInheritanceException;

}
