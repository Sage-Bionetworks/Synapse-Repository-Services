package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.List;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageService {

	public MessageToUser create(String username, MessageToUser toCreate)
			throws NotFoundException;

	public PaginatedResults<MessageBundle> getInbox(String username,
			List<MessageStatusType> inclusionFilter, MessageSortBy sortBy,
			boolean descending, long limit, long offset, String urlPath)
			throws NotFoundException;

	public PaginatedResults<MessageToUser> getOutbox(String username,
			MessageSortBy sortBy, boolean descending, long limit, long offset,
			String urlPath) throws NotFoundException;

	public MessageToUser getMessage(String username, String messageId)
			throws NotFoundException;

	public MessageToUser forwardMessage(String username, String messageId,
			MessageRecipientSet recipients) throws NotFoundException;

	public PaginatedResults<MessageToUser> getConversation(String username,
			String messageId, MessageSortBy sortBy, boolean descending,
			long limit, long offset, String urlPath)
			throws NotFoundException;

	public void updateMessageStatus(String username, MessageStatus status)
			throws NotFoundException;
	
	public void deleteMessage(String username, String messageId)
			throws NotFoundException;

	public URL getMessageFileRedirectURL(String username, String messageId) throws NotFoundException;

	public MessageToUser createMessageToEntityOwner(String username, String entityId,
			MessageToUser toCreate) throws NotFoundException;

}
