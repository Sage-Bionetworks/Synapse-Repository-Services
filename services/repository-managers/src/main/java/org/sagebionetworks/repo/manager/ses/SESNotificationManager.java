package org.sagebionetworks.repo.manager.ses;

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
	 * @param messageBody The body of the notification received from SES
	 * @throws IllegalArgumentException If the given notification is null or if it cannot be parsed
	 */
	void processMessage(String messageBody);

}
