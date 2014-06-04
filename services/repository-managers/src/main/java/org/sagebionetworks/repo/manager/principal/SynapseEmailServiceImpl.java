package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class SynapseEmailServiceImpl implements SynapseEmailService {
	@Autowired
	private AmazonSimpleEmailService amazonSESClient;
	
	public void sendEmail(SendEmailRequest emailRequest) {
		if (StackConfiguration.isProductionStack()) {
			amazonSESClient.sendEmail(emailRequest);
		}
	}
}
