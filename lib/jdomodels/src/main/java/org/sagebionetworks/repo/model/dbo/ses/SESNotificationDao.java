package org.sagebionetworks.repo.model.dbo.ses;

import org.sagebionetworks.repo.model.ses.SESNotification;

/**
 * DAO layer for the SES notifications
 * 
 * @author Marco
 */
public interface SESNotificationDao {

	/**
	 * Saves the given notification to the database
	 * 
	 * @param notification The SES notification DTO
	 * @return
	 */
	SESNotification create(SESNotification notification);

	/**
	 * Clear all the SES notifications from the database
	 */
	void clearAll();

}
