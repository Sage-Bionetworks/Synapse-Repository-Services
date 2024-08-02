package org.sagebionetworks.repo.model.dbo.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;

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
	 * @return
	 */
	Optional<Webhook> getWebhook(String webhookId);

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
	 * @param verificationCode
	 * @param expiresOn
	 */
	void setVerificationCode(String webhookId, String verificationCode, Instant expiresOn);
	
	/**
	 * Truncate all Webhook data.
	 */
	void truncateAll();

}
