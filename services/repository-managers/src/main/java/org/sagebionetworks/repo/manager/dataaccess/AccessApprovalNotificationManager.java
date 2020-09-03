package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Manager to process revocation and renewal notifications for access approvals
 * 
 * @author Marco Marasca
 */
public interface AccessApprovalNotificationManager {

	// Do not re-send another notification if one was sent already within the last 7 days
	long RESEND_TIMEOUT_DAYS = 7;

	// Fake id for messages to that are not actually created, e.g. in staging we do not send the messages
	// to avoid having duplicate messages sent out
	long NO_MESSAGE_TO_USER = -1;

	// Do not process a change message if it's older than 24 hours
	long CHANGE_TIMEOUT_HOURS = 24;
	
	// Maximum number of recipients allowed in a notification request
	int MAX_NOTIFICATION_REQUEST_RECIPIENTS = 25;

	/**
	 * Process a change message for an access approval to check if a revocation notification should be sent out to the
	 * accessor
	 * 
	 * @param message The change message, only {@link ChangeType#UPDATE} changes on {@link ObjectType#ACCESS_APPROVAL}
	 *                are processed
	 * @throws RecoverableMessageException If the message cannot be processed at this time
	 */
	void processAccessApprovalChange(ChangeMessage message) throws RecoverableMessageException;

	/**
	 * Process an access approval to check if a notification of the given type should be sent out
	 * 
	 * @param notificationType The type of notification to sent for the approval with the given id
	 * @param approvalId       The id of the approval
	 * @throws RecoverableMessageException If the notification cannot be processed at the moment but could be processed
	 *                                     at a later time
	 */
	void processAccessApproval(DataAccessNotificationType notificationType, Long approvalId)
			throws RecoverableMessageException;

	/**
	 * Fetches the list of approvals that expires according to the {@link DataAccessNotificationType#getReminderPeriod()
	 * reminder period} and for which a notification wasn't sent already today
	 * 
	 * @param notificationType The notification type, must be a {@link DataAccessNotificationType#isReminder() reminder}
	 * @param limit The maximum number of approvals to fetch
	 * @return The list of approvals that expires in next {@link DataAccessNotificationType#getReminderPeriod()
	 * reminder period}, excluding approvals for which a notification was sent today or that do not expire
	 */
	List<Long> listSubmitterApprovalsForUnsentReminder(DataAccessNotificationType notificationType, int limit);
	
	/**
	 * Fetches the list of notifications according to the given request.
	 * 
	 * @param user The user making the request, must be an ACT member
	 * @param request The request specification
	 * @return The response containing the list of notifications
	 */
	AccessApprovalNotificationResponse listNotificationsRequest(UserInfo user, AccessApprovalNotificationRequest request);

}
