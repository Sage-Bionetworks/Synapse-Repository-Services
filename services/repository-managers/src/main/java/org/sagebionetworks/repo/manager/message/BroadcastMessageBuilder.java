package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.model.message.ChangeType;

public interface BroadcastMessageBuilder {

	/**
	 * Build an email message for the given objectId and changeType.
	 * @param objectId
	 * @param changeType
	 * @return
	 */
	BroadcastMessage buildMessage(String objectId, ChangeType changeType);

}
