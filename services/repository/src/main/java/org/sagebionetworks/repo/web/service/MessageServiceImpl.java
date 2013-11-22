package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageServiceImpl implements MessageService {

	@Autowired
	private MessageManager messageManager;

	@Autowired
	private UserManager userManager;

	@Override
	public MessageToUser create(String username, MessageToUser toCreate)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return messageManager.createMessage(userInfo, toCreate);
	}

	@Override
	public PaginatedResults<MessageBundle> getInbox(String username,
			List<MessageStatusType> inclusionFilter, MessageSortBy sortBy,
			boolean descending, long limit, long offset, String urlPath)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		QueryResults<MessageBundle> messages = messageManager.getInbox(
				userInfo, inclusionFilter, sortBy, descending, limit, offset);
		return new PaginatedResults<MessageBundle>(urlPath,
				messages.getResults(), messages.getTotalNumberOfResults(),
				offset, limit, sortBy.name(), !descending);
	}

	@Override
	public PaginatedResults<MessageToUser> getOutbox(String username,
			MessageSortBy sortBy, boolean descending, long limit, long offset,
			String urlPath) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		QueryResults<MessageToUser> messages = messageManager.getOutbox(
				userInfo, sortBy, descending, limit, offset);
		return new PaginatedResults<MessageToUser>(urlPath,
				messages.getResults(), messages.getTotalNumberOfResults(),
				offset, limit, sortBy.name(), !descending);
	}

	@Override
	public MessageToUser getMessage(String username, String messageId)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return messageManager.getMessage(userInfo, messageId);
	}

	@Override
	public MessageToUser forwardMessage(String username, String messageId,
			MessageRecipientSet recipients) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		return messageManager.forwardMessage(userInfo, messageId, recipients);
	}

	@Override
	public PaginatedResults<MessageToUser> getConversation(String username,
			String messageId, MessageSortBy sortBy, boolean descending,
			long limit, long offset, String urlPath) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		QueryResults<MessageToUser> messages = messageManager.getConversation(
				userInfo, messageId, sortBy, descending, limit, offset);
		return new PaginatedResults<MessageToUser>(urlPath,
				messages.getResults(), messages.getTotalNumberOfResults(),
				offset, limit, sortBy.name(), !descending);
	}

	@Override
	public void updateMessageStatus(String username, MessageStatus status)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		messageManager.markMessageStatus(userInfo, status);
	}

	@Override
	public void deleteMessage(String username, String messageId)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(username);
		messageManager.deleteMessage(userInfo, messageId);
	}

}
