package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageDAO {
	
	/**
	 * Retrieves a message by ID
	 * 
	 * Note: the message body is not downloaded
	 */
	public Message getMessage(String messageId) throws NotFoundException;

	/**
	 * Saves the message information so that it can be processed by a worker
	 * 
	 * @param dto This relevant IDs of this object may be changed
	 */
	public Message createMessage(Message dto);
	
	/**
	 * Retrieves all messages (subject to limit and offset) within a given thread
	 * Regardless of the sender or receiver
	 * @param sortBy What value to sort the results by
	 */
	public List<Message> getThread(String threadId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages within the thread
	 * Regardless of the sender or receiver
	 */
	public long getThreadSize(String threadId);
	
	/**
	 * Retrieves all messages (subject to limit and offset) within a given thread, visible to the user
	 * @param sortBy What value to sort the results by
	 */
	public List<Message> getThread(String threadId, String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages within the thread
	 */
	public long getThreadSize(String threadId, String userId);
	
	/**
	 * Retrieves all messages (subject to limit and offset) received by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageBundle> getReceivedMessages(String userId, List<MessageStatusType> included, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages received by the user
	 */
	public long getNumReceivedMessages(String userId, List<MessageStatusType> included);
	
	/**
	 * Retrieves all messages (subject to limit and offset) sent by the user
	 * @param sortBy What value to sort the results by
	 */
	public List<Message> getSentMessages(String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages sent by the user
	 */
	public long getNumSentMessages(String userId);
	
	/**
	 * Sends a message to a user by adding an UNREAD message tied to the user
	 */
	public void registerMessageRecipient(String messageId, String userId);
	
	/**
	 * Marks a message with the given status
	 */
	public void updateMessageStatus(String messageId, String userId, MessageStatusType status);
}
