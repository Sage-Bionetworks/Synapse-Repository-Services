package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;

/**
 * This object will observe the TransactionalMessenger and sent those messages to an AWS topic.
 * This is the mechanism we are using to notify listeners to changes that occur in Synapse.
 * 
 * @author John
 *
 */
public interface RepositoryMessagePublisher extends TransactionalMessengerObserver {
	
	/**
	 * Get the name of the topic where the messages are published.
	 * 
	 * @return
	 */
	public String getTopicName();
	
	/**
	 * The ARN for the topic where messages are published.
	 * 
	 * @return
	 */
	public String getTopicArn();
}
