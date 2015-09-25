package org.sagebionetworks.repo.manager.principal;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public interface SynapseEmailService {
	
	public void sendRawEmail(SendRawEmailRequest sendRawEmailRequest);

}
