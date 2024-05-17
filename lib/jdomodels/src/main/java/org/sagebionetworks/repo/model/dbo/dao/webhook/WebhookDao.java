package org.sagebionetworks.repo.model.dbo.dao.webhook;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;

public interface WebhookDao {

	/**
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	Webhook createWebhook(Long userId, CreateOrUpdateWebhookRequest request);
	
	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	Webhook getWebhook(String webhookId);
	
	/**
	 * 
	 * @param webhookId
	 * @return
	 */
	Optional<String> getWebhookOwnerForUpdate(String webhookId);
	
	/**
	 * 
	 * @param userId
	 * @param webhookId
	 * @param request
	 * @return
	 */
	Webhook updateWebhook(Long userId, String webhookId, CreateOrUpdateWebhookRequest request);
	
	/**
	 * 
	 * @param webhookId
	 */
	void deleteWebhook(String webhookId);
	
	/**
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Webhook> listUserWebhooks(Long userId, long limit, long offset);
	
	/**
	 * 
	 * @param objectId
	 * @return
	 */
	List<Webhook> listVerifiedAndEnabledWebhooksForObjectId(String objectId, WebhookObjectType webhookObjectType);
	
	/**
	 * 
	 * @param webhookId
	 * @param verificationStatus
	 */
	void setWebhookVerificationStatus(String webhookId, boolean verificationStatus);
}
