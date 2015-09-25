package org.sagebionetworks.repo.manager.principal;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public interface SynapseEmailService {
	
	public void sendEmail(SendEmailRequest emailRequest);

	public void sendRawEmail(SendRawEmailRequest sendRawEmailRequest);

}
