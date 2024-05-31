package org.sagebionetworks.repo.model.dbo.webhook;

import org.sagebionetworks.repo.model.webhook.WebhookVerification;

public interface WebhookVerificationDao {

	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	WebhookVerification createWebhookVerification(WebhookVerification verification);

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
	Long incrementAttempts(String webhookId);
	
	/**
	 * Truncate all WebhookVerificationData data.
	 */
	void truncateAll();
}
