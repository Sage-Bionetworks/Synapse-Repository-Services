package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.MessageUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessage;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sun.tools.javac.util.Pair;


public class MessageManagerImpl implements MessageManager {
	
	@Autowired
	private MessageDAO messageDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	public String getThreadId(String messageId) throws NotFoundException {
		return messageDAO.getMessageThread(messageId);
	}
	
	@Override
	public String checkMessage(UserInfo userInfo, Message dto) {
		DBOMessage dbo = MessageUtils.convertDTO(dto);
		
		// Will be auto generated
		dbo.setMessageId(-1L);
		
		// Can be inferred from UserInfo
		dbo.setCreatedBy(-2L);
		
		// Check to see if all the required fields of the messages are present
		try {
			MessageUtils.validateDBO(dbo);
		
			// Check permissions
			switch (dto.getRecipientType()) {
			case PRINCIPAL:
				for (String principalId : dto.getRecipients()) {
					checkSendPermissionForPrincipal(userInfo, principalId);
				}
				break;
			case ENTITY:
				checkSendPermissionForEntity(userInfo, dto.getRecipients());
				break;
			default:
				return "Unsupported recipient type: " + dto.getRecipientType();
			}
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Message createMessage(UserInfo userInfo, Message dto) throws NotFoundException {
		// Make sure the sender is correct
		dto.setCreatedBy(userInfo.getIndividualGroup().getId());
		
		// Check permissions
		if (dto.getRecipientType() == ObjectType.ENTITY) {
			checkSendPermissionForEntity(userInfo, dto.getRecipients());
		}
		
		dto = messageDAO.createMessage(dto);
		
		// Process the message in this transaction if the recipient list is only one element long
		if (dto.getRecipients().size() == 1) {
			List<String> errors = sendMessage(dto.getMessageId());
			if (errors.size() > 0) {
				throw new IllegalArgumentException(StringUtils.join(errors, "\n"));
			}
		}
		return dto;
	}

	@Override
	public QueryResults<Message> getCommentThread(UserInfo userInfo, String threadId,
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException {
		// For comment threads, permissions are tied to the entity
		Pair<ObjectType, String> obj = messageDAO.getObjectOfThread(threadId);
		if (!authorizationManager.canAccess(userInfo, obj.snd, obj.fst, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException(userInfo.getIndividualGroup().getName() + " lacks read access to the entity (" + obj.snd + ")");
		}
		
		List<Message> dtos = messageDAO.getThread(threadId, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getThreadSize(threadId);
		return new QueryResults<Message>(dtos, totalMessages);
	}

	@Override
	public QueryResults<Message> getMessageThread(UserInfo userInfo, String threadId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<Message> dtos = messageDAO.getThread(threadId, userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getThreadSize(threadId, userInfo.getIndividualGroup().getId());
		return new QueryResults<Message>(dtos, totalMessages);
	}

	@Override
	public QueryResults<MessageBundle> getInbox(UserInfo userInfo,  
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageBundle> dtos = messageDAO.getReceivedMessages(userInfo.getIndividualGroup().getId(), 
				included, sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumReceivedMessages(userInfo.getIndividualGroup().getId(), included);
		return new QueryResults<MessageBundle>(dtos, totalMessages);
	}

	@Override
	public QueryResults<Message> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<Message> dtos = messageDAO.getSentMessages(userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumSentMessages(userInfo.getIndividualGroup().getId());
		return new QueryResults<Message>(dtos, totalMessages);
	}

	@Override
	public void markMessageStatus(UserInfo userInfo, String messageId, MessageStatusType status) {
		messageDAO.updateMessageStatus(messageId, userInfo.getIndividualGroup().getId(), status);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public List<String> sendMessage(String messageId) throws NotFoundException {
		List<String> errors = new ArrayList<String>();
		
		Message dto = messageDAO.getMessage(messageId);
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		
		// Tie the message to an existing thread if the replyTo field is specified
		String replyTo = dto.getReplyTo();
		if (replyTo != null) {
			messageDAO.getMessage(replyTo);
			String threadId;
			try {
				threadId = messageDAO.getMessageThread(replyTo);
			} catch (NotFoundException e) {
				// The message exists but isn't in a thread
				// So create a thread
				threadId = messageDAO.registerMessageToThread(replyTo, null);
			}
			messageDAO.registerMessageToThread(messageId, threadId);
		}
		
		switch (dto.getRecipientType()) {
		case PRINCIPAL:
			Set<String> recipients = new HashSet<String>();
			for (String principalId : dto.getRecipients()) {
				// Check permissions
				try {
					checkSendPermissionForPrincipal(userInfo, principalId);
					recipients.add(principalId);
				} catch (NotFoundException e) {
					errors.add(e.getMessage());
					continue;
				} catch (UnauthorizedException e) {
					errors.add(e.getMessage());
					continue;
				}
			}
			
			// Mark each message as sent
			recipients = expandRecipientListToIndividuals(recipients);
			for (String user : recipients) {
				messageDAO.registerMessageRecipient(messageId, user);
			}
			break;
		case ENTITY:
			// Check permissions
			try {
				checkSendPermissionForEntity(userInfo, dto.getRecipients());
			} catch (NotFoundException e) {
				errors.add(e.getMessage());
				break;
			} catch (UnauthorizedException e) {
				errors.add(e.getMessage());
				break;
			}
			
			// Create a new thread if the replyTo field is not specified
			if (replyTo == null) {
				String entityThreadId = messageDAO.registerMessageToThread(messageId, null);
				messageDAO.registerThreadToObject(entityThreadId, ObjectType.ENTITY, dto.getRecipients().iterator().next());
			}
			break;
		default:
			throw new IllegalArgumentException("Unsupported recipient type: " + dto.getRecipientType());
		}
		
		// Remove the now-sent message from the queue
		//TODO the queue's implementation is still up in the air
		// Note: this may be the job of this method's caller, which is not implemented yet 
		
		return errors;
	}
	
	/**
	 * Throws an exception if the user does not have sufficient permission to send messages to a principal
	 */
	private void checkSendPermissionForPrincipal(UserInfo userInfo, String principalId)
			throws NotFoundException {
		UserGroup ug = userGroupDAO.get(principalId);
		
		if (!ug.getIsIndividual()) {
			if (!authorizationManager.canAccess(userInfo, principalId, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)) {
				throw new UnauthorizedException(userInfo.getIndividualGroup().getName()
						+ " may not send messages to the group (" + principalId + ")");
			}
		}
	}

	/**
	 * Expands a set of principals into a set of individuals
	 */
	private Set<String> expandRecipientListToIndividuals(Set<String> recipients) throws NotFoundException {
		Set<String> individuals = new HashSet<String>();
		for (UserGroup ug : userGroupDAO.get(new ArrayList<String>(recipients))) {
			if (ug.getIsIndividual()) {
				individuals.add(ug.getId());
			} else { 
				for (UserGroup member : groupMembersDAO.getMembers(ug.getId())) {
					individuals.add(member.getId());
				}
			}
		}
		return individuals;
	}
	
	/**
	 * Throws an exception if the user does not have sufficient permission to send messages to an entity
	 */
	private void checkSendPermissionForEntity(UserInfo userInfo, Set<String> recipients) 
			throws NotFoundException {
		if (recipients == null || recipients.size() != 1) {
			throw new IllegalArgumentException("Only one entity can be commented upon at a time");
		}
		
		String objectId = recipients.iterator().next();
		if (!authorizationManager.canAccess(userInfo, objectId, ObjectType.ENTITY, ACCESS_TYPE.SEND_MESSAGE)) {
			throw new UnauthorizedException(userInfo.getIndividualGroup().getName() + 
					" may not comment on the entity (" + objectId + ")");
		}
	}
	
}
