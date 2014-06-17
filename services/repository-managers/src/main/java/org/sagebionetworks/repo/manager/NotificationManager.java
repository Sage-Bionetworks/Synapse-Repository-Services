package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.UserInfo;
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
	 * @param from
	 * @param to
	 * @param subject
	 * @param message
	 * @param contentType e.g. text/plain or text/html
	 * @throws NotFoundException 
	 */
	public void sendNotification(UserInfo userInfo, Set<String> to, String subject, String message, String contentType) throws NotFoundException;
	
}
