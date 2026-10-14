package org.sagebionetworks.repo.manager.principal;

import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

public interface SynapseEmailService {
	
	public void sendEmail(SendEmailRequest emailRequest);

	public void sendRawEmail(SendRawEmailRequest sendRawEmailRequest);

}
