package org.sagebionetworks.repo.manager.ses;

import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.springframework.stereotype.Service;

/**
 * Complaints are tricky, we should restrict the sender, not the recipient in this case. For
 * example: fraud@fakecompany.com sends fake email to user@syanpse.org and that message gets marked
 * as fraud. Under the current system user@syanpse.org would no longer receive emails even though
 * this is a completely valid email address.
 * 
 * @author Marco
 */
@Service
public class EmailQuarantineComplaintProvider implements EmailQuarantineProvider {

	@Override
	public SESNotificationType getSupportedType() {
		return SESNotificationType.COMPLAINT;
	}

	@Override
	public QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId) {
		return QuarantinedEmailBatch.EMPTY_BATCH;
	}

}
