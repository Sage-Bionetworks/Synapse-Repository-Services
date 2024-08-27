package org.sagebionetworks.repo.model.dbo.webhook;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;

public interface WebhookDao {

	Webhook createWebhook(Long userId, CreateOrUpdateWebhookRequest request);
		
	Optional<Webhook> getWebhook(String webhookId, boolean forUpdate);
	
	Webhook updateWebhook(String webhookId, CreateOrUpdateWebhookRequest request);

	void deleteWebhook(String webhookId);

	List<Webhook> listUserWebhooks(Long userId, long limit, long offset);
	
	/**
	 * 
	 * @param ids
	 * @param objectType
	 * @param eventType
	 * @param limit
	 * @param offset
	 * @return The list of webhooks that are subscribed to the given object ids for the given event type that are verified and enabled
	 */
	List<Webhook> listWebhooksForObjectIds(List<Long> ids, SynapseObjectType objectType, SynapseEventType eventType, long limit, long offset);

	DBOWebhookVerification getWebhookVerification(String webhookId);
	
	/**
	 * Updates the verification code of the webhook. This method will generate a unique messageId and reset the status to PENDING and clear the message.
	 * 
	 * @param webhookId
	 * @param verificationCode
	 * @param expiresOn
	 * @return
	 */
	DBOWebhookVerification setWebhookVerificationCode(String webhookId, String verificationCode, Instant expiresOn);
	
	/**
	 * Updates the verification status of a webhook with an optional message.
	 * 
	 * @param webhookId
	 * @param status
	 * @param message
	 * @param messageId
	 * @return
	 */
	DBOWebhookVerification setWebhookVerificationStatus(String webhookId, WebhookVerificationStatus status, String message);
	
	/**
	 * 
	 * @param webhookId
	 * @param messageId
	 * @return The verification status of the webhook with the given id and messageId if it matches
	 */
	Optional<WebhookVerificationStatus> getWebhookVerificationStatus(String webhookId, String messageId);
	
	/**
	 * Updates the verification status of a webhook with an optional message iff the given codeMessageId matches. Note that this method won't throw if no update is perfomed.
	 * 
	 * @param webhookId
	 * @param status
	 * @param message
	 * @param messageId
	 * @return
	 */
	void setWebhookVerificationStatusIfMessageIdMatch(String webhookId, String codeMessageId, WebhookVerificationStatus status, String message);
	
	DBOWebhookAllowedDomain addAllowedDomainPattern(String pattern);
	
	List<String> getAllowedDomainsPatterns();
	
	void truncateAll();


}
