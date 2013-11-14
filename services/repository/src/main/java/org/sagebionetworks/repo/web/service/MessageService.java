package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageToUser;

public interface MessageService {

	public MessageToUser create(String username, MessageToUser toCreate);

	public PaginatedResults<MessageBundle> getInbox(String username,
			String orderBy, boolean descending, long limit, long offset);

	public PaginatedResults<MessageToUser> getOutbox(String username, String orderBy,
			boolean descending, long limit, long offset);

	public MessageToUser getMessage(String username, String messageId);

	public MessageToUser forwardMessage(String username, String messageId,
			MessageRecipientSet recipients);

	public PaginatedResults<MessageToUser> getConversation(String username,
			String messageId, String orderBy, boolean descending, long limit,
			long offset);

	public MessageStatus getMessageStatus(String username, String messageId);

	public void updateMessageStatus(String username, MessageStatus status);

	public PaginatedResults<MessageToUser> getCommentThread(String username,
			String id, String orderBy, boolean descending, long limit,
			long offset);

	public MessageToUser commentOnThread(String username, String id, MessageToUser toCreate);

}
