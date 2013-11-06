package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;


public interface MessageManager {

	/**
	 * Retrieves the thread ID of a message
	 */
	public String getThreadId(String messageId) throws NotFoundException;
	
	/**
	 * Saves the message so that it can be processed by other queries
	 * and by workers to send the message 
	 * 
	 * @throws NotFoundException If the user is commenting on an Entity that does not exist
	 */
	public Message createMessage(UserInfo userInfo, Message dto) throws NotFoundException;
	
	/**
	 * Retrieves all messages within a thread tied to an entity
	 * The entity must be accessible to the user
	 */
	public QueryResults<Message> getCommentThread(UserInfo userInfo, String threadId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset) throws NotFoundException;
	
	/**
	 * Retrieves all messages within a thread that are visible to the user
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public QueryResults<Message> getMessageThread(UserInfo userInfo, String threadId, String userId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Retrieves all unread messages received by the user
	 * 
	 * Note: The behavior of received messages will be eventually consistent
	 */
	public QueryResults<MessageBundle> getInbox(UserInfo userInfo, String userId, 
			List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Retrieves all messages sent by the user
	 */
	public QueryResults<Message> getOutbox(UserInfo userInfo, String userId, 
			MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Changes the status of the user's message 
	 */
	public void markMessageStatus(UserInfo userInfo, String messageId, String userId, MessageStatusType status);
	
	/**
	 * Takes an existing message and processes it, 
	 * updating tables and sending emails where necessary and permitted
	 * 
	 * Note: This should only be used by the worker in charge of sending messages.  
	 */
	public void sendMessage(String messageId) throws NotFoundException;
}
