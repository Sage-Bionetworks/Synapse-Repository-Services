package org.sagebionetworks.repo.model.dbo.dao.webhook;

import org.sagebionetworks.repo.model.webhook.WebhookVerification;

public interface WebhookVerificationDao {

	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	WebhookVerification createWebhookVerification(WebhookVerification webhookVerification);
	
	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	WebhookVerification getWebhookVerification(String webhookId);
	
	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	long incrementAttempts(String webhookId);
	
	/**
	 * 
	 */
	void pruneExpiredWebhookVerifications();
}
