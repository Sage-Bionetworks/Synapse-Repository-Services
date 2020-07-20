package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Instant;
import java.util.Optional;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.message.MessageToUser;

public interface DataAccessNotificationDao {

	/**
	 * Register a notification that was sent for a given access requirement and recipient
	 * 
	 * @param type The type of notification
	 * @param requirementId The id of the {@link AccessRequirement}
	 * @param recipientId The id of the {@link UserInfo recipient}
	 * @param accessApprovalId The id of the {@link AccessApproval} that led to this notification
	 * @param messageId The id of the {@link MessageToUser}
	 * @param sentOn When the notification was sent
	 */
	void registerNotification(DataAccessNotificationType type, Long requirementId, Long recipientId, Long accessApprovalId, Long messageId, Instant sentOn);

	/**
	 * Fetch the (last) sent on for the notification of the given type for the
	 * approval with the given id.
	 * 
	 * @param type The type of notification
	 * @param requirementId The id of the {@link AccessRequirement}
	 * @param recipientId The id of the {@link UserInfo recipient}
	 * @return An optional containing the sentOn instant for the notification, empty if no notification could be found
	 */
	Optional<Instant> getSentOn(DataAccessNotificationType type, Long requirementId, Long recipientId);
	
	// For testing
	
	void clear();

	Optional<String> getEtag(DataAccessNotificationType type, Long requirementId, Long recipientId);
}
