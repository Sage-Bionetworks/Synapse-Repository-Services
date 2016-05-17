package org.sagebionetworks.repo.manager.message;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.utils.HttpClientHelperException;

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
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws ClientProtocolException 
	 */
	SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber) throws ClientProtocolException, JSONException, IOException, HttpClientHelperException;

}
