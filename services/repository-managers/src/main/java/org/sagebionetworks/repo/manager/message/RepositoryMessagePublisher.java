package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ModificationMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;

import com.amazonaws.services.sns.AmazonSNSClient;

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
	public String getTopicName(ObjectType type);
	
	/**
	 * The ARN for the topic where messages are published.
	 * 
	 * @return
	 */
	public String getTopicArn(ObjectType type);
	
	/**
	 * Used by tests to inject a mock client.
	 * @param awsSNSClient
	 */
	public void setAwsSNSClient(AmazonSNSClient awsSNSClient);
	
	
	/**
	 * Quartz will fire this method on a timer.  This is where we actually publish the data. 
	 */
	public void timerFired();
	
	/**
	 * Publish a message to its topic and register the message as sent.
	 * 
	 * @param message
	 * @return 
	 */
	public void publishToTopic(ChangeMessage message);

	/**
	 * Publish a message to the modification topic.
	 * 
	 * @param message
	 * @return
	 */
	public void publishToModificationTopic(ModificationMessage message);
}
