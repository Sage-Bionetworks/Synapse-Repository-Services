package org.sagebionetworks.repo.model;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;

public interface MessageDAO {
	
	/**
	 * Retrieves a message by ID
	 */
	public MessageToUser getMessage(String messageId) throws NotFoundException;
	
	/**
	 * Saves the message information so that it can be processed by a worker, allows to override the recipients notification settings
	 */
	MessageToUser createMessage(MessageToUser dto, boolean overrideNotificationSettings);
	
	/**
	 * Changes the etag of a message
	 */
	public void touch(String messageId);
	
	/**
	 * Mark the transmission status of the message as complete
	 * 
	 * @param messageId
	 */
	public void updateMessageTransmissionAsComplete(String messageId);
	
	/**
	 * Retrieves all messages (subject to limit and offset) within a given thread, visible to the user
	 * @param sortBy What value to sort the results by
	 */
	public List<MessageToUser> getConversation(String rootMessageId, String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages within the thread
	 */
	public long getConversationSize(String rootMessageId, String userId);
	
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
	public List<MessageToUser> getSentMessages(String userId, MessageSortBy sortBy, boolean descending, long limit, long offset);
	
	/**
	 * Returns the number of messages sent by the user
	 */
	public long getNumSentMessages(String userId);
	
	/**
	 * Marks a user as a recipient of a message
	 * The status of the message defaults to UNREAD
	 * 
	 * Note: This 'recipient status' is disctinct from the message's 'transmission status'.
	 * Note: This operation occurs in a separate transaction (REQUIRES_NEW)
	 */
	public void createMessageStatus(String messageId, String userId, MessageStatusType status);
	
	/**
	 * Marks a message within the recipient's inbox with the given status, doing so in a new, isolated transaction
	 * Note: this 'recipient status' is disctinct from the message's 'transmission status'.
	 * 
	 * @return Did the update succeed?
	 */
	boolean updateMessageStatus(MessageStatus status);
	
	/**
	 * Deletes a message.  Only used for test cleanup.
	 */
	public void deleteMessage(String messageId);

	/**
	 * Returns true if there is at least one recipient of the message
	 * @throws NotFoundException 
	 */
	public boolean getMessageSent(String messageId) throws NotFoundException;

	/**
	 * Checks how many messages a user has created over a given interval (from present time)
	 * If the given threshold is met or exceeded, this returns false
	 */
	public boolean canCreateMessage(String userId, long maxNumberOfNewMessages,
			long messageCreationInterval);
	
	/**
	 * Checks if the given file handle has been sent (or was intended to be sent) to the given UserGroups
	 * If so, then the user or group should be allowed to download the file associated with the file handle
	 */
	public boolean canSeeMessagesUsingFileHandle(Set<Long> userGroups, String fileHandleId);
	
	/**
	 * True if the message with the given id should override the user notification settings
	 */
	boolean overrideNotificationSettings(String messageId) throws NotFoundException;
	
	// For testing
	void truncateAll();

}
