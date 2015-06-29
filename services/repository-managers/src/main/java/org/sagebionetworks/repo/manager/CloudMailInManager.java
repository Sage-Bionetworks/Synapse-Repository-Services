package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CloudMailInManager {
	
	/**
	 * Check the from, to to see if the message is valid.
	 * 
	 * @param header
	 * @throws IllegalArgumentException if not valid
	 */
	public void authorizeMessage(AuthorizationCheckHeader header);
	
	/**
	 * Craft a message based on content in the CloudMailIn format.
	 * See http://docs.cloudmailin.com/
	 * The returned message must be sent.
	 * @param message
	 * @param notificationUnsubscribeEndpoint
	 * @throws NotFoundException
	 */
	public MessageToUserAndBody convertMessage(Message message, String notificationUnsubscribeEndpoint) throws NotFoundException;


}
