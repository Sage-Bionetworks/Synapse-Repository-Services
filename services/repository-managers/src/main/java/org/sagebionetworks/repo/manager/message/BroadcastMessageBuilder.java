package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public interface BroadcastMessageBuilder {

	/**
	 * Get the topic that the messages should be broadcast too.
	 * 
	 * @return
	 */
	Topic getBroadcastTopic();
	
	/**
	 * Build an email request for a given subscriber.
	 * 
	 * @param subscriber
	 * @return
	 */
	SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber);

}
