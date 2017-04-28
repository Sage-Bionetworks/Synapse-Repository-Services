package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.PublishResults;

/**
 * This class will rebroadcast existing change messages to their corresponding
 * topics.
 * 
 */
public interface MessageSyndication {

	/**
	 * Rebroadcast all change messages.
	 */
	public void rebroadcastAllChangeMessages();

	/**
	 * Rebroadcast limit change messages from given change number (inclusive).
	 * @return next startChangeNumber or -1 if last batch
	 */
	public long rebroadcastChangeMessages(Long startChangeNumber, Long limit);
	
	/**
	 * Return the last change message number
	 */
	public long getCurrentChangeNumber();
	
	/**
	 * Rebroadcast change messages to a queue starting from a given change number (inclusive).
	 * @param queueName - The name of the target queue.
	 * @param type - Only messages of this ObjectType will be rebroadcast.
	 * @param startChangeNumber - The change number to start from (inclusive).  To rebroadcast all messages set this to zero.
	 * @return - The total number of messages sent
	 */
	PublishResults rebroadcastChangeMessagesToQueue(String queueName, ObjectType type, Long startChangeNumber, Long limit);
	
	
	/**
	 * List changes messages.
	 * @param startChangeNumber
	 * @param type - This is an optional filter, when set, only messages of this type will be returned.
	 * @param limit
	 * @return
	 */
	ChangeMessages listChanges(Long startChangeNumber, ObjectType type, Long limit);
	
}
