package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This manager supports sending system generated messages to users.
 * 
 * @author brucehoff
 *
 */
public interface NotificationManager {

	public static final String TEXT_PLAIN_MIME_TYPE = "text/plain";

	/**
	 * 
	 * @param userInfo
	 * @param mtu
	 * @param message
	 * @param stopOnFailure If true and any of the message notification fail, bubble up the exception
	 * @throws NotFoundException
	 */
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages, boolean stopOnFailure) throws NotFoundException;

}
