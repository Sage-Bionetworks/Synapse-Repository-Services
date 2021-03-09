package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;

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
	 * @return An optional containing the notification data, empty if not found
	 */
	Optional<DBODataAccessNotification> find(DataAccessNotificationType type, Long requirementId, Long recipientId);

	/**
	 * Fetches the notification of the given type, for the given requirement and
	 * recipient and lock for update.
	 * 
	 * @param type          The type of notification
	 * @param requirementId The id of the {@link AccessRequirement}
	 * @param recipientId   The id of the {@link UserInfo recipient}
	 * @return An optional containing the notification data, empty if not found
	 */
	Optional<DBODataAccessNotification> findForUpdate(DataAccessNotificationType type, Long requirementId, Long recipientId);

	/**
	 * Fetches all the notifications for the requirement with the given id and the list of recipients
	 * 
	 * @param requirementId The id of an {@link AccessRequirement}
	 * @param recipientIds The list of recipient ids
	 * @return The list of notifications for the given requirement and list of recipients
	 */
	List<DBODataAccessNotification> listForRecipients(Long requirementId, List<Long> recipientIds);
	
	/**
	 * Fetches the list of approval ids for which a reminder wasn't sent on the given date
	 * 
	 * @param notificationType The notification type, must be a {@link DataAccessNotificationType#isReminder() a reminder}
	 * @param sentOn The date when the reminder should have been sent
	 * @param limit The max number of approval to fetch
	 * @return The list of approval ids for which a notification should have been sent on the given date and didn't
	 */
	List<Long> listSubmmiterApprovalsForUnSentReminder(DataAccessNotificationType notificationType, LocalDate sentOn, int limit);

	// For testing

	void truncateAll();
}
