package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;


/**
 * A manager for servicing Webhooks
 * 
 * @author lmoenning
 */
public interface WebhookManager {
	
	/**
	 * Create a new Webhook object.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	Webhook createWebhook(UserInfo userInfo, Webhook webhook);
	
	/**
	 * Get the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @return
	 */
	Webhook getWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * Update the corresponding Webhook with the provided Webhook. 
	 * 
	 * @param userInfo
	 * @param toUpdate
	 * @return
	 */
	Webhook updateWebhook(UserInfo userInfo, Webhook toUpdate); 
	
	/**
	 * Delete the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 */
	void deleteWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * Verify the Webhook of the provided webhookId by the provided VerifyWebhookRequest. 
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @param request
	 * @return
	 */
	VerifyWebhookResponse verifyWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request);
	
	/**
	 * List all webhookIds for a Synapse user. 
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	ListUserWebhooksResponse listUserWebhooks(UserInfo userInfo, ListUserWebhooksRequest request);
	
	/**
	 * List all Webhooks for an objectId that are: verified, enabled, and the owner has read permission on objectId.
	 * 
	 * @param objectId
	 * @return
	 */
	List<Webhook> listSendableWebhooksForObjectId(String objectId, WebhookObjectType webhookObjectType);
	
	/**
	 * Generate and send a new WebhookVerification for the Webhook of the provided webhookId. 
	 * 
	 * @param userId
	 * @param webhookId
	 */
	void generateAndSendWebhookVerification(Long userId, String webhookId);
}
