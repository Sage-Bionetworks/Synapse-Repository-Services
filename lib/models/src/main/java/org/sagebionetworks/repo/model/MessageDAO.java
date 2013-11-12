package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.sun.tools.javac.util.Pair;

public interface MessageDAO {
	
	/**
	 * Retrieves a message by ID
	 */
	public Message getMessage(String messageId) throws NotFoundException;

	/**
	 * Saves the message information so that it can be processed by a worker
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
	 * Marks a user as a recipient of a message
	 * The status of the message is initially UNREAD
	 */
	public void registerMessageRecipient(String messageId, String userId);
	
	/**
	 * Marks a message within the user's inbox with the given status
	 */
	public void updateMessageStatus(String messageId, String userId, MessageStatusType status);
	
	/**
	 * Returns the thread ID associated with a message
	 *  
	 * @throws NotFoundException If the message does not exist
	 *   or if the message is not associated with any thread
	 */
	public String getMessageThread(String messageId) throws NotFoundException;
	
	/**
	 * Adds a message to a thread
	 * 
	 * @param threadId Set to null to generate a new thread for the message
	 * @throws IllegalArgumentException If the message already belongs to a thread
	 * @return The thread ID 
	 */
	public String registerMessageToThread(String messageId, String threadId) throws IllegalArgumentException;
	
	/**
	 * Returns the thread IDs linked to the object
	 */
	public List<String> getThreadsOfObject(ObjectType objectType, String objectId);
	
	/**
	 * Returns the object linked to the thread
	 */
	public Pair<ObjectType, String> getObjectOfThread(String threadId) throws NotFoundException;
	
	/**
	 * Assigns a thread to an object
	 * 
	 * @throws IllegalArgumentException If the thread already belongs to an object
	 */
	public void registerThreadToObject(String threadId, ObjectType objectType, String objectId) throws IllegalArgumentException;
}
