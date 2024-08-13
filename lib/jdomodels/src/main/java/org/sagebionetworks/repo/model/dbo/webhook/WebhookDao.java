package org.sagebionetworks.repo.model.dbo.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;

public interface WebhookDao {

	/**
	 * 
	 * @param webhook
	 * @return
	 */
	Webhook createWebhook(Long userId, CreateOrUpdateWebhookRequest request);
		
	/**
	 * 
	 * @param webhookId
	 * @param forUpdate
	 * @return
	 */
	Optional<Webhook> getWebhook(String webhookId, boolean forUpdate);
	
	/**
	 * 
	 * @param webhook
	 * @return
	 */
	Webhook updateWebhook(String webhookId, CreateOrUpdateWebhookRequest request);

	/**
	 * 
	 * @param webhookId
	 */
	void deleteWebhook(String webhookId);

	/**
	 * 
	 * @param userId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Webhook> listUserWebhooks(Long userId, long limit, long offset);

	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	DBOWebhookVerification getWebhookVerification(String webhookId);
	
	/**
	 * 
	 * @param verificationCode
	 * @param expiresOn
	 */
	DBOWebhookVerification setWebhookVerificationCode(String webhookId, String verificationCode, Instant expiresOn);
	
	/**
	 * 
	 * @param webhookId
	 * @param status
	 * @param message
	 * @return
	 */
	DBOWebhookVerification setWebhookVerificationStatus(String webhookId, WebhookVerificationStatus status, String message);
	
	/**
	 * Truncate all Webhook data.
	 */
	void truncateAll();


}
