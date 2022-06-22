package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.message.MessageToUser;

public interface TemplatedMessageSender {
	
	/**
	 * Send a {@link MessageToUser} to using the provided template.
	 * 
	 * @param template
	 * @return
	 */
	public MessageToUser sendMessage(MessageTemplate template);

}
