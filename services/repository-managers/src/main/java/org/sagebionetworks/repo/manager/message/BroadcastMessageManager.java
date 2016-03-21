package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;

/**
 * The Broadcast manager is responsible for sending an email to users subscribed to certain events.
 * This manager is generic and used to send message to for any type of change message.
 *
 */
public interface BroadcastMessageManager {

	/**
	 * Main entry point to broadcast a given change message.
	 * @param user
	 * @param changeMessage
	 */
	public void broadcastMessage(UserInfo user,
			ProgressCallback<ChangeMessage> progressCallback,
			ChangeMessage changeMessage);

}
