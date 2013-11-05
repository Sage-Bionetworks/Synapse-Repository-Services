package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.RecipientBundle;

public interface MessageService {

	public Message create(String username, String replyTo, Message toCreate);

	public PaginatedResults<MessageBundle> getInbox(String username,
			String orderBy, boolean descending, long limit, long offset);

	public PaginatedResults<Message> getOutbox(String username, String orderBy,
			boolean descending, long limit, long offset);

	public Message getMessage(String username, String messageId);

	public Message forwardMessage(String username, String messageId,
			RecipientBundle recipients);

	public PaginatedResults<Message> getMessageThread(String username,
			String messageId, String orderBy, boolean descending, long limit,
			long offset);

	public MessageStatus getMessageStatus(String username, String messageId);

	public void updateMessageStatus(String username, MessageStatus status);

	public PaginatedResults<Message> getCommentThread(String username,
			String id, String orderBy, boolean descending, long limit,
			long offset);

	public Message commentOnThread(String username, String id, Message toCreate);

}
