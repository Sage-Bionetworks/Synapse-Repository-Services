package org.sagebionetworks.repo.manager.ses;

import java.util.Set;

import org.sagebionetworks.repo.model.principal.EmailQuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

/**
 * Provider for notifications of type {@link SESNotificationType#BOUNCE} to handle hard and soft bounces
 * 
 * @author Marco
 */
@Service
public class EmailQuarantineBounceProvider implements EmailQuarantineProvider {

	// By default adds to the quarantine transient types for one day
	static final Long RETRY_TIMEOUT = 24 * 60 * 60 * 1000L;

	private static final String UNKNOWN_TYPE = "UNKNOWN";

	// See https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#bounce-types
	private static final Set<String> PERMANENT_QUARANTINE_TYPES = ImmutableSet.of("PERMANENT");
	private static final Set<String> TEMPORARY_QUARANTINE_TYPES = ImmutableSet.of(UNKNOWN_TYPE, "UNDETERMINED");

	@Override
	public SESNotificationType getSupportedType() {
		return SESNotificationType.BOUNCE;
	}

	@Override
	public QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId) {

		QuarantinedEmailBatch batch = new QuarantinedEmailBatch();

		// We only care about the bounce type
		String notificationSubtype = notificationDetails.getSubType().orElse(UNKNOWN_TYPE).trim().toUpperCase();
		EmailQuarantineReason reason = null;

		if (PERMANENT_QUARANTINE_TYPES.contains(notificationSubtype)) {
			reason = EmailQuarantineReason.PERMANENT_BOUNCE;
		} else if (TEMPORARY_QUARANTINE_TYPES.contains(notificationSubtype)) {
			reason = EmailQuarantineReason.OTHER;
			batch.withExpirationTimeout(RETRY_TIMEOUT);
		} else {
			return batch;
		}
		
		String reasonDetails = null;
		
		if (notificationDetails.getReason().isPresent()) {
			reasonDetails = notificationDetails.getReason().get().toUpperCase();
		}
		
		for (SESJsonRecipient recipient : notificationDetails.getRecipients()) {
			if (recipient.getEmailAddress() != null) {
				
				QuarantinedEmail quarantinedEmail = new QuarantinedEmail(recipient.getEmailAddress(), reason)
						.withSesMessageId(sesMessageId)
						.withReasonDetails(reasonDetails);
				
				batch.add(quarantinedEmail);	
			}
		}

		return batch;

	}

}
