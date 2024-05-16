package org.sagebionetworks.repo.model.dbo.dao.webhook;

import org.sagebionetworks.repo.model.webhook.WebhookVerification;

public interface WebhookVerificationDao {

	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	WebhookVerification createWebhookVerification(Long userId, String webhookId);
	
	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	WebhookVerification getWebhookVerification(String webhookId);
	
	/**
	 * 
	 */
	void pruneExpiredWebhookVerifications();
}
