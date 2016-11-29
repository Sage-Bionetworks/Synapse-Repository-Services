package org.sagebionetworks.repo.manager.message;

import java.io.IOException;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.Topic;

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
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws ClientProtocolException 
	 * @throws MarkdownClientException 
	 */
	SendRawEmailRequest buildEmailForSubscriber(Subscriber subscriber)
			throws ClientProtocolException, JSONException, IOException, MarkdownClientException;

	/**
	 * Build an email request for a given subscriber.
	 * 
	 * @param user
	 * @return
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws ClientProtocolException 
	 * @throws MarkdownClientException 
	 */
	SendRawEmailRequest buildEmailForNonSubscriber(UserNotificationInfo user)
			throws ClientProtocolException, JSONException, IOException, MarkdownClientException;

	/**
	 * 
	 * @return a set of userIds that related to this message
	 */
	Set<String> getRelatedUsers();
}
