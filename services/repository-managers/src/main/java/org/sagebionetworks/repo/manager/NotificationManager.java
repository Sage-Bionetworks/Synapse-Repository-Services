package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;

/**
 * This manager supports sending system generated messages to users, the messages are sent from a noreply address 
 * and delivery failures are ignored (e.g. no delivery failure notification is sent back to the original sender).
 * 
 * @author brucehoff
 *
 */
public interface NotificationManager {

	/**
	 * Send the given list of messages, the generated message will be send as a notification (from a system noreply sender).
	 * <p/>
	 * Notifications are sent asynchronously, if the messages could not be delivered failure notifications are ignored
	 * 
	 * @param userInfo The user requesting the notification to be sent
	 * @param messages The list of messages to be sent
	 */
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages);

}
