package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.web.NotFoundException;

public interface CloudMailInManager {
	/**
	 * Craft a message based on content in the CloudMailIn format.
	 * See http://docs.cloudmailin.com/
	 * The returned message must be sent.
	 * @param message
	 * @throws NotFoundException
	 */
	public MessageToUserAndBody convertMessage(Message message) throws NotFoundException;


}
