package org.sagebionetworks.repo.model.dbo.ses;

import org.sagebionetworks.repo.model.ses.SESNotificationRecord;

/**
 * DAO layer for the SES notifications
 * 
 * @author Marco
 */
public interface SESNotificationDao {

	/**
	 * Saves the given notification to the database, the id, instanceNumber and createOn will be set automatically
	 * 
	 * @param notification The SES notification DTO
	 * @return The saved notification
	 */
	SESNotificationRecord saveNotification(SESNotificationRecord notification);

	/**
	 * @param sesMessageId The id returned by SES when sending an email
	 * @return The count of notifications received for the given message id
	 */
	Long countBySesMessageId(String sesMessageId);

	/**
	 * Clear all the SES notifications from the database
	 */
	void clearAll();

}
