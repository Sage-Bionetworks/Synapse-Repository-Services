package org.sagebionetworks.repo.manager.webhook;

import java.util.Optional;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;

/**
 * A manager for servicing Webhooks
 * 
 * @author lmoenning
 */
public interface WebhookManager {

	String MSG_ATTR_WEBHOOK_ID = "WebhookId";
	String MSG_ATTR_WEBHOOK_ENDPOINT = "WebhookEndpoint";
	String MSG_ATTR_WEBHOOK_OWNER_ID = "WebhookOwnerId";
	String MSG_ATTR_WEBHOOK_MESSAGE_TYPE = "WebhookMessageType";
	String MSG_ATTR_WEBHOOK_MESSAGE_ID = "WebhookMessageId";

	/**
	 * Create a new Webhook object.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request);

	/**
	 * Update the corresponding Webhook with the provided Webhook.
	 * 
	 * @param userInfo
	 * @param toUpdate
	 * @return
	 */
	Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request);
	
	/**
	 * Get the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @return
	 */
	Webhook getWebhook(UserInfo userInfo, String webhookId);


	/**
	 * Delete the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 */
	void deleteWebhook(UserInfo userInfo, String webhookId);

	/**
	 * Manual verification of a webhook endpoint
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @param request
	 * @return
	 */
	VerifyWebhookResponse verifyWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request);
	
	/**
	 * Send a new verification code to the webhook endpoint
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @return
	 */
	Webhook generateWebhookVerificationCode(UserInfo userInfo, String webhookId);

	/**
	 * List all webhookIds for a Synapse user.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	ListUserWebhooksResponse listUserWebhooks(UserInfo userInfo, ListUserWebhooksRequest request);
		
	/**
	 * Process the given change message in order to dispatch the supported changes to the subscribed webhooks
	 * 
	 * @param change
	 */
	void processChangeMessage(ChangeMessage change);
	
	/**
	 * 
	 * @param webhookId
	 * @param messageId
	 * @return The verification status for the webhook with the given id that matches the given messageId
	 */
	Optional<WebhookVerificationStatus> getWebhookVerificationStatus(String webhookId, String messageId);
	
	/**
	 * Updates the status of the webhook verification if the given messageId matches
	 * 
	 * @param webhookId
	 * @param messageId
	 * @param status
	 * @param verificationMessage
	 */
	void updateWebhookVerificationStatus(String webhookId, String messageId, WebhookVerificationStatus status, String verificationMessage);
}
