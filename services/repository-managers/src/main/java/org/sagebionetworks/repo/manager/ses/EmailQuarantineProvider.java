package org.sagebionetworks.repo.manager.ses;

import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

/**
 * Provide the list of {@link QuarantinedEmail}s from the {@link SESJsonNotificationDetails details} of a SES
 * Notification.
 * 
 * @author Marco
 */
public interface EmailQuarantineProvider {

	/**
	 * @return The {@link SESNotificationType} supported by this provider
	 */
	SESNotificationType getSupportedType();

	/**
	 * This method is invoked in order to get a batch of {@link QuarantinedEmail} from the given
	 * {@link SESJsonNotificationDetails}, it is never invoked when the recipients list is empty
	 * 
	 * @param notificationDetails The {@link SESJsonNotificationDetails details} of a SES notification that contains the
	 *                            recipients, the notification subType and the reason
	 * @param sesMessageId        Optional identifier from SES
	 * @return A {@link QuarantinedEmailBatch} to be added to the quarantine, an empty batch if no recipient is to be added
	 */
	QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId);

}
