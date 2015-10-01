package org.sagebionetworks.repo.manager;

import java.util.List;

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
	 * Craft messages based on content in the CloudMailIn format.
	 * See http://docs.cloudmailin.com/
	 * Nominally the returned list has one Synapse message and zero or one error notifications to send to the message creator.
	 * The returned messages must be sent.
	 * @param message
	 * @param notificationUnsubscribeEndpoint
	 * @throws NotFoundException
	 */
	public List<MessageToUserAndBody> convertMessage(Message message, String notificationUnsubscribeEndpoint) throws NotFoundException;


}
