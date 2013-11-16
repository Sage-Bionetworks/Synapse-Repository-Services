package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.MessageDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
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
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private AuthorizationManager authorizationManager;

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto) {
		// Make sure the sender is correct
		dto.setCreatedBy(userInfo.getIndividualGroup().getId());
		
		dto = messageDAO.createMessage(dto);
		
		// If the recipient list is only one element long, 
		// process and send the message in this transaction 
		if (dto.getRecipients().size() == 1) {
			List<String> errors;
			try {
				errors = sendMessage(dto.getId());
			} catch (NotFoundException e) {
				throw new DatastoreException("Could not find a message that was created in the same transaction");
			}
			if (errors.size() > 0) {
				throw new IllegalArgumentException(StringUtils.join(errors, "\n"));
			}
		}
		return dto;
	}

	@Override
	public QueryResults<MessageToUser> getConversation(UserInfo userInfo, String associatedMessageId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException {
		MessageToUser dto = messageDAO.getMessage(associatedMessageId);
		String rootMessageId = dto.getInReplyToRoot();
		
		List<MessageToUser> dtos = messageDAO.getConversation(rootMessageId, userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getConversationSize(rootMessageId, userInfo.getIndividualGroup().getId());
		return new QueryResults<MessageToUser>(dtos, totalMessages);
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
	public QueryResults<MessageToUser> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) {
		List<MessageToUser> dtos = messageDAO.getSentMessages(userInfo.getIndividualGroup().getId(), 
				sortBy, descending, limit, offset);
		long totalMessages = messageDAO.getNumSentMessages(userInfo.getIndividualGroup().getId());
		return new QueryResults<MessageToUser>(dtos, totalMessages);
	}

	@Override
	public void markMessageStatus(UserInfo userInfo, String messageId, MessageStatusType status) {
		messageDAO.updateMessageStatus(messageId, userInfo.getIndividualGroup().getId(), status);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public List<String> sendMessage(String messageId) throws NotFoundException {
		List<String> errors = new ArrayList<String>();
		
		MessageToUser dto = messageDAO.getMessage(messageId);
		UserInfo userInfo = userManager.getUserInfo(Long.parseLong(dto.getCreatedBy()));
		
		// From the list of intended recipients, filter out the un-permitted recipients
		Set<String> recipients = new HashSet<String>();
		for (String principalId : dto.getRecipients()) {
			UserGroup ug;
			try {
				ug = userGroupDAO.get(principalId);
			} catch (NotFoundException e) {
				errors.add(e.getMessage());
				continue;
			}
			
			// Check permissions to send to non-individuals
			if (!ug.getIsIndividual()
					&& !authorizationManager.canAccess(userInfo, principalId, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)) {
				errors.add(userInfo.getIndividualGroup().getName()
						+ " may not send messages to the group (" + principalId + ")");
				continue;
			}
			
			// The principal is a valid recipient
			if (ug.getIsIndividual()) {
				recipients.add(principalId);
			} else {
				// Expand non-individuals into individuals
				for (UserGroup member : groupMembersDAO.getMembers(principalId)) {
					recipients.add(member.getId());
				}
			}
		}
		
		// Mark each message as sent
		for (String user : recipients) {
			//TODO check the recipient's settings
			//TODO send emails if necessary
			messageDAO.createMessageStatus(messageId, user);
		}
		
		//TODO Remove the now-sent message from the queue
		// Note: the queue's implementation is still up in the air
		// Note: this may be the job of this method's caller, which is not implemented yet 
		
		return errors;
	}
	
}
