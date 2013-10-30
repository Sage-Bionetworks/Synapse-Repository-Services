package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageDAO {
	
	public enum MESSAGE_SORT_BY {
		SEND_DATE, 
		SUBJECT, 
		SENDER
	}
	
	/**
	 * Retrieves a message by ID
	 * 
	 * Note: the message body is not downloaded
	 */
	public Message getMessage(String messageId) throws NotFoundException;

	/**
	 * Saves the message information so that it can be processed by a worker
	 */
	public void saveMessage(Message dto);
	
	/**
	 * Retrieves all messages (subject to limit and offset) within a given thread
	 * @param sortBy What value to sort the results by
	 */
	public List<Message> getThread(String threadId, MESSAGE_SORT_BY sortBy, boolean descending, long limit, long offset) 
			throws NotFoundException;
	
	/**
	 * Returns the number of messages within the thread, regardless of visibility
	 */
	public long getThreadSize(String threadId) throws NotFoundException;
	
	/**
	 * Retrieves all messages (subject to limit and offset) received by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageBundle> getReceivedMessages(String userId, MESSAGE_SORT_BY sortBy, boolean descending, long limit, long offset) 
			throws NotFoundException;
	
	/**
	 * Returns the number of messages received by the user
	 */
	public long getNumReceivedMessages(String userId) throws NotFoundException;
	
	/**
	 * Retrieves all messages (subject to limit and offset) sent by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<Message> getSentMessages(String userId, MESSAGE_SORT_BY sortBy, boolean descending, long limit, long offset) 
			throws NotFoundException;
	
	/**
	 * Returns the number of messages sent by the user
	 */
	public long getNumSentMessages(String userId) throws NotFoundException;
	
	/**
	 * Sends a message to a user by adding an UNREAD message tied to the user
	 */
	public void registerMessageRecipient(String messageId, String userId) throws NotFoundException;
	
	/**
	 * Marks a message with the given status
	 */
	public void updateMessageStatus(String messageId, String userId, MessageStatusType status) throws NotFoundException;
}
