package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager to process revocation and renewal notifications for access approvals
 * 
 * @author Marco Marasca
 */
public interface AccessApprovalNotificationManager {

	/**
	 * Process a change message for an access approval to check if a revocation
	 * notification should be sent out to the accessor
	 * 
	 * @param user    The user creating the notification, must be an admin
	 * @param message The change message, only {@link ChangeType#UPDATE} changes on
	 *                {@link ObjectType#ACCESS_APPROVAL} are processed
	 * @throws RecoverableMessageException If the message cannot be processed at
	 *                                     this time
	 */
	void processAccessApprovalChangeMessage(UserInfo user, ChangeMessage message) throws RecoverableMessageException;

}
