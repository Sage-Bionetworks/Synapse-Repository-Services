package org.sagebionetworks.repo.manager.ses;

import java.util.Set;

import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

import com.google.common.collect.ImmutableSet;

/**
 * Provider for notifications of type {@link SESNotificationType#BOUNCE} to handle hard and soft bounces
 * 
 * @author Marco
 */
public class EmailQuarantineBounceProvider implements EmailQuarantineProvider {

	// By default adds to the quarantine transient types for one day
	private static final Long RETRY_TIMEOUT = 24 * 60 * 60 * 1000L;

	private static final String UNKNOWN_TYPE = "UNKNOWN";

	// See https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#bounce-types
	private static final Set<String> PERMANENT_QUARANTINE_TYPES = ImmutableSet.of("PERMANENT");
	private static final Set<String> TRANSIENT_QUARANTINE_TYPES = ImmutableSet.of(UNKNOWN_TYPE, "TRANSIENT", "UNDETERMINED");

	@Override
	public SESNotificationType getSupportedType() {
		return SESNotificationType.BOUNCE;
	}

	@Override
	public QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId) {

		QuarantinedEmailBatch batch = new QuarantinedEmailBatch().withSesMessageId(sesMessageId);

		// We only care about the bounce type
		String notificationSubtype = notificationDetails.getSubType().orElse(UNKNOWN_TYPE).toUpperCase();

		if (PERMANENT_QUARANTINE_TYPES.contains(notificationSubtype)) {
			batch.withReason(QuarantineReason.PERMANENT_BOUNCE);
		} else if (TRANSIENT_QUARANTINE_TYPES.contains(notificationSubtype)) {
			batch.withReason(QuarantineReason.TRANSIENT_BOUNCE).withExpirationTimeout(RETRY_TIMEOUT);
		} else {
			return batch;
		}

		notificationDetails.getRecipients().forEach(recipient -> {
			if (recipient.getEmailAddress() == null) {
				return;
			}

			batch.add(recipient.getEmailAddress());
		});

		return batch;

	}

}
