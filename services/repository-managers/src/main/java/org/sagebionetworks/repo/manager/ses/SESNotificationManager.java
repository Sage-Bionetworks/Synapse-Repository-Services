package org.sagebionetworks.repo.manager.ses;

import org.sagebionetworks.repo.model.ses.SESJsonNotification;

/**
 * Manager layer for the bounce and complaint notifications received from SES
 * 
 * @author Marco
 */
public interface SESNotificationManager {

	/**
	 * Process the given notification received from SES and saves a record to the database. If the notification is an hard
	 * bounce adds the recipients to the list of quarantined addresses
	 * 
	 * @param notification The parsed notification, should contain the original untouched notification body
	 * @throws IllegalArgumentException If the given notification is null or the body is not present
	 */
	void processNotification(SESJsonNotification notification);

}
