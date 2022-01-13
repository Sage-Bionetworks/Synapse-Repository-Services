package org.sagebionetworks.repo.manager.ses;

import org.sagebionetworks.repo.model.ses.SESJsonNotification;

/**
 * Manager layer for the bounce and complaint notifications received from SES
 * 
 * @author Marco
 */
public interface SESNotificationManager {

	/**
	 * Process the given message received from SES and saves a record to the database. If the notification is an hard
	 * bounce adds the recipients to the list of quarantined addresses
	 * 
	 * @param notification The notification received from SES
	 * @param messageBody The original SQS message body
	 * @throws IllegalArgumentException If the given notification is null or if it cannot be parsed
	 */
	void processMessage(SESJsonNotification notification, String messageBody);

}
