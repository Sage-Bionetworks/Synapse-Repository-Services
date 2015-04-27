package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This manager supports sending system generated messages to users.
 * 
 * @author brucehoff
 *
 */
public interface NotificationManager {

	/**
	 * 
	 * @param userInfo
	 * @param mtu
	 * @param message
	 * @throws NotFoundException
	 */
	public void sendNotification(UserInfo userInfo, MessageToUser mtu,
			String message) throws NotFoundException;
	
	
	public static final String TEXT_PLAIN_MIME_TYPE = "text/plain";

}
