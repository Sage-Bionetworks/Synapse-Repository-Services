package org.sagebionetworks.repo.web.service;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.CloudMailInManager;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.MessageToUserUtils;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageServiceImpl implements MessageService {

	@Autowired
	private MessageManager messageManager;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private CloudMailInManager cloudMailInManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Override
	public MessageToUser create(Long userId, MessageToUser toCreate)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return messageManager.createMessageWithThrottle(userInfo, MessageToUserUtils.setUserGeneratedMessageFooter(toCreate));
	}

	@Override
	public void create(Message toCreate, String notificationUnsubscribeEndpoint) throws Exception {
		List<MessageToUserAndBody> mtubs = cloudMailInManager.convertMessage(toCreate, notificationUnsubscribeEndpoint);
		if (mtubs.isEmpty()) 
			return;
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(mtubs.get(0).getMetadata().getCreatedBy()));
		for (MessageToUserAndBody message : mtubs) {
			if (message.getMetadata().getRecipients().isEmpty()) continue;
			FileHandle fileHandle = fileHandleManager.createCompressedFileFromString(
					userInfo.getId().toString(), new Date(), message.getBody(), message.getMimeType());

			message.getMetadata().setFileHandleId(fileHandle.getId());
			messageManager.createMessage(userInfo, message.getMetadata());
		}
	}
	
	@Override
	public void authorize(AuthorizationCheckHeader ach) {
		cloudMailInManager.authorizeMessage(ach);
	}

	@Override
	public PaginatedResults<MessageBundle> getInbox(Long userId,
			List<MessageStatusType> inclusionFilter, MessageSortBy sortBy,
			boolean descending, long limit, long offset, String urlPath)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<MessageBundle> messages = messageManager.getInbox(
				userInfo, inclusionFilter, sortBy, descending, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(messages, limit, offset);
	}

	@Override
	public PaginatedResults<MessageToUser> getOutbox(Long userId,
			MessageSortBy sortBy, boolean descending, long limit, long offset,
			String urlPath) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<MessageToUser> messages = messageManager.getOutbox(
				userInfo, sortBy, descending, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(messages, limit, offset);
	}

	@Override
	public MessageToUser getMessage(Long userId, String messageId)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return messageManager.getMessage(userInfo, messageId);
	}

	@Override
	public MessageToUser forwardMessage(Long userId, String messageId,
			MessageRecipientSet recipients) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return messageManager.forwardMessage(userInfo, messageId, recipients);
	}

	@Override
	public PaginatedResults<MessageToUser> getConversation(Long userId,
			String messageId, MessageSortBy sortBy, boolean descending,
			long limit, long offset, String urlPath) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<MessageToUser> messages = messageManager.getConversation(
				userInfo, messageId, sortBy, descending, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(messages, limit, offset);
	}

	@Override
	public void updateMessageStatus(Long userId, MessageStatus status)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		messageManager.markMessageStatus(userInfo, status);
	}

	@Override
	public void deleteMessage(Long userId, String messageId)
			throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		messageManager.deleteMessage(userInfo, messageId);
	}

	@Override
	public String getMessageFileRedirectURL(Long userId, String messageId) throws NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return messageManager.getMessageFileRedirectURL(userInfo, messageId);
	}

	@Override
	public MessageToUser createMessageToEntityOwner(Long userId, String entityId,
			MessageToUser toCreate) throws NotFoundException, ACLInheritanceException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return messageManager.createMessageToEntityOwner(userInfo, entityId, toCreate);
	}

}
