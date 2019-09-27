package org.sagebionetworks.repo.model.ses;

import java.util.List;
import java.util.Optional;

/**
 * Common interface that provides unified details about the components exposed by the SES
 * notification
 * 
 * @author Marco
 *
 */
public interface SESJsonNotificationDetails {

	/**
	 * @return The identifier assigned by SES to the feefback
	 */
	String getFeedbackId();

	/**
	 * @return The optional type categorization of the notification (e.g. the type of bounce)
	 */
	Optional<String> getSubType();

	/**
	 * @return The optional reason detail for the notification (e.g. the sub type of the bounce, or the
	 *         abuse code of the complaint)
	 */
	Optional<String> getReason();

	/**
	 * @return The list of recipients the notifications refers to
	 */
	List<SESJsonRecipient> getRecipients();

}
