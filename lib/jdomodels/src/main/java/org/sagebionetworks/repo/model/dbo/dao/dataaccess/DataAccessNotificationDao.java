package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Optional;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;

public interface DataAccessNotificationDao {

	/**
	 * Creates a new notification
	 * 
	 * @param notification The notification to store
	 * 
	 * @return The newly created DBO object
	 */
	DBODataAccessNotification create(DBODataAccessNotification notification);

	/**
	 * Updates the given notification, the id of the notification must be present
	 * 
	 * @param notification The notification to update
	 * @return The updated notification
	 */
	void update(Long id, DBODataAccessNotification notification);

	/**
	 * Fetches the notification of the given type, for the given requirement and
	 * recipient.
	 * 
	 * @param type          The type of notification
	 * @param requirementId The id of the {@link AccessRequirement}
	 * @param recipientId   The id of the {@link UserInfo recipient}
	 * @param forUpdate     True if the row on a match should be locked for update
	 * @return An optional containing the notification data, empty if not found
	 */
	Optional<DBODataAccessNotification> findForUpdate(DataAccessNotificationType type, Long requirementId, Long recipientId);

	// For testing

	void clear();
}
