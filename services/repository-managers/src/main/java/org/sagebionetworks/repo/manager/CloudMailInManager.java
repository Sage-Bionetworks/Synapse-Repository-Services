package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CloudMailInManager {
	/**
	 * This is the email domain used with CloudMailIn
	 */
	public static final String EMAIL_SUFFIX_LOWER_CASE = "@synapse.org";
	
	/**
	 * Craft a message based on content in the CloudMailIn format.
	 * See http://docs.cloudmailin.com/
	 * The returned message must be sent.
	 * @param message
	 * @throws NotFoundException
	 */
	public MessageToUserAndBody convertMessage(Message message, String notificationUnsubscribeEndpoint) throws NotFoundException;


}
