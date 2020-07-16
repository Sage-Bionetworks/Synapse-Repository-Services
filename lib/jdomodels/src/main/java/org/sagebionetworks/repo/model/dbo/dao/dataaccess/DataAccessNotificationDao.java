package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Instant;
import java.util.Optional;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.message.MessageToUser;

public interface DataAccessNotificationDao {

	/**
	 * Register the message sent for the access approval with the given id, if a message already exists for the
	 * given access type and approval updates the message id, etag etc.
	 * 
	 * @param type             The notification type
	 * @param accessApprovalId The id of the {@link AccessApproval}
	 * @param messageId        The id of the {@link MessageToUser} that was sent
	 */
	void registerSent(DataAccessNotificationType type, Long requirementId, Long recipientId, Long accessApprovalId, Long messageId, Instant sentOn);

	/**
	 * Fetch the (last) sent on for the notification of the given type for the
	 * approval with the given id.
	 * 
	 * @param type            The notification type
	 * @param accessAprovalId The id of the {@link AccessApproval}
	 * @return An optional containing the sentOn instant of the notification for the
	 *         approval with the given id if such a notification was create, empty
	 *         otherwise
	 */
	Optional<Instant> getSentOn(DataAccessNotificationType type, Long requirementId, Long recipientId);

}
