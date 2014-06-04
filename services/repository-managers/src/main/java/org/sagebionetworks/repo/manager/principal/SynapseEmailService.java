package org.sagebionetworks.repo.manager.principal;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public interface SynapseEmailService {
	
	public void sendEmail(SendEmailRequest emailRequest);

}
