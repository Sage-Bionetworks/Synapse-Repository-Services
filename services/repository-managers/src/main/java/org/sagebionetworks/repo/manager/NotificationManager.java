package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @throws ServiceUnavailableException 
	 */
	public void sendNotifications(UserInfo userInfo, List<MessageToUserAndBody> messages) throws NotFoundException, ServiceUnavailableException;
	
	
	public static final String TEXT_PLAIN_MIME_TYPE = "text/plain";

}
