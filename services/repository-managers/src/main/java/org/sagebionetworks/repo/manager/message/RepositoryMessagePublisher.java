package org.sagebionetworks.repo.manager.message;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;

import com.amazonaws.services.sns.AmazonSNS;

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
	public void setAwsSNSClient(AmazonSNS awsSNSClient);
	
	
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
	 * Publish a batch of change messages to a topic. All ChangeMessages in the
	 * batch must be of the given Object type. The batch size cannot be larger
	 * than the maximum number of change messages that can be written to a
	 * single SQS message body.
	 * 
	 * @param type The object type of the batch.
	 * @param batch
	 */
	public void publishBatchToTopic(ObjectType type, List<ChangeMessage> batch);
}
