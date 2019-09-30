package org.sagebionetworks.repo.manager.ses;

import java.util.Set;

import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class EmailQuarantineComplaintProvider implements EmailQuarantineProvider {

	static final String UNKNOWN_REASON = "UNKNOWN";
	// A complaint does not have a subtype, but a reason (See
	// https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#complaint-object)
	private Set<String> COMPLAINT_REASONS = ImmutableSet.of("ABUSE", "AUTH-FAILURE", "FRAUD", "VIRUS", "OTHER");

	@Override
	public SESNotificationType getSupportedType() {
		return SESNotificationType.COMPLAINT;
	}

	@Override
	public QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId) {
		QuarantinedEmailBatch batch = new QuarantinedEmailBatch()
				.withReason(QuarantineReason.COMPLAINT)
				.withSesMessageId(sesMessageId);

		String complaintReason = notificationDetails.getReason().orElse(UNKNOWN_REASON).toUpperCase();
		
		// We do not process other complaint types
		if (!COMPLAINT_REASONS.contains(complaintReason)) {
			return batch;
		}

		batch.withReasonDetails(complaintReason);

		notificationDetails.getRecipients().forEach(recipient -> {
			if (recipient.getEmailAddress() == null) {
				return;
			}
			batch.add(recipient.getEmailAddress());
		});

		return batch;
	}

}
