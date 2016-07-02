package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/**
 * A BroadcastMessageBuilder instance is created with specific information for a
 * change event. It builds SendRawEmailRequest related to this change event for users.
 *
 */
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

	/**
	 * Build an email request for a given subscriber.
	 * 
	 * @param user
	 * @return
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws ClientProtocolException 
	 */
	SendRawEmailRequest buildEmailForNonSubscriber(UserNotificationInfo user)
			throws ClientProtocolException, JSONException, IOException, HttpClientHelperException;

	/**
	 * 
	 * @return a set of userIds that related to this message
	 */
	Set<String> getRelatedUsers();
}
