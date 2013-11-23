package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;


public interface MessageManager {
	
	/**
	 * Retrieves a single message by ID.  
	 * The user must be either the sender or *intended* recipient of the message.  
	 * Otherwise, an UnauthorizedException is thrown.  
	 */
	public MessageToUser getMessage(UserInfo userInfo, String messageId) throws NotFoundException;
	
	/**
	 * Saves the message so that it can be processed by other queries.
	 * If the message is going to exactly one recipient, then the message will be sent in this transaction  
	 * and any failures will be propagated immediately.
	 * </br> 
	 * If the message is going to more than one recipient, a worker will asynchronously process the message.
	 * In case of failure, the user will be notified via bounce message.  
	 */
	public MessageToUser createMessage(UserInfo userInfo, MessageToUser dto);

	/**
	 * Saves an existing message so that it can be delivered to the given set of recipients
	 */
	public MessageToUser forwardMessage(UserInfo userInfo, String messageId,
			MessageRecipientSet recipients) throws NotFoundException;
	
	/**
	 * Retrieves all messages within the same conversation as the associated message.
	 * All returned messages will be visible to the user
	 * (i.e. the user is either the sender or receiver of the messages)
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public QueryResults<MessageToUser> getConversation(UserInfo userInfo, String associatedMessageId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException;
	
	/**
	 * Retrieves all messages received by the user
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public QueryResults<MessageBundle> getInbox(UserInfo userInfo, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Retrieves all messages sent by the user
	 */
	public QueryResults<MessageToUser> getOutbox(UserInfo userInfo, 
			MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Changes the status of the user's message 
	 */
	public void markMessageStatus(UserInfo userInfo, MessageStatus status) throws NotFoundException;
	
	/**
	 * Takes an existing message and processes it, 
	 * updating tables and sending emails where necessary and permitted.
	 * </br>
	 * Non-fatal errors will be caught and their error messages will be returned in a list.
	 * It is the caller's responsibility to send a bounce message to the user.
	 */
	public List<String> sendMessage(String messageId) throws NotFoundException;
	
	/**
	 * Deletes a message, only accessible to admins
	 */
	public void deleteMessage(UserInfo userInfo, String messageId);
}
