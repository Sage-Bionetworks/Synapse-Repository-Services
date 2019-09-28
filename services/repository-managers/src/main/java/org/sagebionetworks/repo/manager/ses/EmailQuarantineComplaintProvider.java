package org.sagebionetworks.repo.manager.ses;

import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

public class EmailQuarantineComplaintProvider implements EmailQuarantineProvider {

	@Override
	public SESNotificationType getSupportedType() {
		return SESNotificationType.COMPLAINT;
	}

	@Override
	public QuarantinedEmailBatch getQuarantinedEmails(SESJsonNotificationDetails notificationDetails, String sesMessageId) {
		return new QuarantinedEmailBatch();
	}

}
