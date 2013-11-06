package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.RecipientType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class MessageManagerImpl implements MessageManager {
	
	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public String getThreadId(String messageId) throws NotFoundException {
		Message dto = messageDAO.getMessage(messageId);
		return dto.getThreadId();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Message createMessage(UserInfo userInfo, Message dto) throws NotFoundException {
		// Make sure the sender is correct
		dto.setCreatedBy(userInfo.getIndividualGroup().getId());
		
		// There are a few extra requirements for creating messages directed towards entities
		boolean isCommentThreadNew = false;
		String nodeId = null;
		if (dto.getRecipientType() == RecipientType.ENTITY) {
			if (dto.getRecipients() == null || dto.getRecipients().size() != 1) {
				throw new IllegalArgumentException("Exactly one recipient must be specified for comments on Entities");
			}
			nodeId = dto.getRecipients().iterator().next();
			
			if (!authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.SEND_MESSAGE)) {
				throw new UnauthorizedException(userInfo.getIndividualGroup().getName() + " may not comment on the entity");
			}
			
			try {
				String threadId = messageDAO.getThreadOfNode(nodeId);
				
				if (!threadId.equals(dto.getThreadId())) {
					if (dto.getThreadId() == null) {
						dto.setThreadId(threadId);
					} else {
						// If the threadId field is hidden from the end user, 
						// then this error should never be hit
						throw new IllegalArgumentException("Ambiguous target of message: " +
								"specified thread ID (" + dto.getThreadId() + ") " +
								"does not match thread ID of Entity (" + threadId + ")");
					}
				}
			} catch (NotFoundException e) {
				// In this case, the entity is not tied to any thread
				// So the thread must be linked to the entity
				isCommentThreadNew = true;
			}
		}
		
		dto = messageDAO.createMessage(dto);
		if (isCommentThreadNew) {
			messageDAO.registerThreadToNode(dto.getThreadId(), nodeId);
		}
		return dto;
	}

	@Override
	public QueryResults<Message> getCommentThread(UserInfo userInfo, String threadId,
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException {
		// For comment threads, permissions are tied to the entity
		String nodeId = messageDAO.getNodeOfThread(threadId);
		if (!authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userInfo.getIndividualGroup().getName() + " lacks read access to the requested object.");
		}
		
		List<Message> dtos = messageDAO.getThread(threadId, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getThreadSize(threadId);
		return new QueryResults<Message>(dtos, totalMessages);
	}
	
	/**
	 * Throws an UnauthorizedException if the user is not the sender/receiver of messages 
	 */
	private void ensureUserIsCreatorOrAdmin(UserInfo userInfo, String userId) {
		if (!authorizationManager.isUserCreatorOrAdmin(userInfo, userId)) {
			throw new UnauthorizedException(userInfo.getIndividualGroup().getName() + " lacks access to the requested object(s)");
		}
	}

	@Override
	public QueryResults<Message> getMessageThread(UserInfo userInfo, String threadId, String userId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		ensureUserIsCreatorOrAdmin(userInfo, userId);
		
		List<Message> dtos = messageDAO.getThread(threadId, userId, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getThreadSize(threadId, userId);
		return new QueryResults<Message>(dtos, totalMessages);
	}

	@Override
	public QueryResults<MessageBundle> getInbox(UserInfo userInfo, String userId, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		ensureUserIsCreatorOrAdmin(userInfo, userId);
		
		List<MessageBundle> dtos = messageDAO.getReceivedMessages(userId, included, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumReceivedMessages(userId, included);
		return new QueryResults<MessageBundle>(dtos, totalMessages);
	}

	@Override
	public QueryResults<Message> getOutbox(UserInfo userInfo, String userId,
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		ensureUserIsCreatorOrAdmin(userInfo, userId);
		
		List<Message> dtos = messageDAO.getSentMessages(userId, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumSentMessages(userId);
		return new QueryResults<Message>(dtos, totalMessages);
	}

	@Override
	public void markMessageStatus(UserInfo userInfo, String messageId,
			String userId, MessageStatusType status) {
		ensureUserIsCreatorOrAdmin(userInfo, userId);
		
		messageDAO.updateMessageStatus(messageId, userId, status);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void sendMessage(String messageId) throws NotFoundException {
		Message dto = messageDAO.getMessage(messageId);
		
		switch (dto.getRecipientType()) {
		case ENTITY:
			// Comments on entities require no extra processing
			break;
		case PRINCIPAL:
			processAndSendMessage(dto);
			break;
		default:
			// There are two safeguards in place to prevent this exception:
			// 1) The RecipientType enumeration
			// 2) An equivalent enumeration in the MESSAGE table
			throw new RuntimeException("Unknown recipient type: " + dto.getRecipientType());
		}
		
		// Remove the now-sent message from the queue
		//TODO the queue's implementation is still up in the air
		// Note: this may be the job of this method's caller, which is not implemented yet 
	}

	/**
	 * Helper for {@link #sendMessage(String)}
	 */
	private void processAndSendMessage(Message dto) {
		List<String> recipients = new ArrayList<String>(dto.getRecipients());
		List<UserGroup> ugs = userGroupDAO.get(recipients);
		for (UserGroup ug : ugs) {
			messageDAO.registerMessageRecipient(dto.getMessageId(), ug.getId());
			
			//TODO send email depending on the user's settings
		}
	}
}
