package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
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
	 * Create a new Webhook object. This object serves as registration for a Synapse user to receive events for the specified objectId. 
	 * The combination of the objectId and invokeEndpoint must be unique for each Webhook. The attribute isEnabled will default to true unless otherwise specified.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request);
	
	/**
	 * Get the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @return
	 */
	Webhook getWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * Update the Webhook corresponding to the provided webhookId. 
	 * 
	 * Note: if the invokeEndpoint is changed upon update or the webhook is reenabled by the user, the user will be required to reverify the webhook. 
	 * If Synapse disables the webhook due to an invalid endpoint, update the endpoint using this service, then reverify with PUT /webhook/{webhookId}/verify. 
	 * The combination of the objectId and invokeEndpoint must be unique for each Webhook.
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @param request
	 * @return
	 */
	Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request); 
	
	/**
	 * Delete the Webhook corresponding to the provided webhookId.
	 * 
	 * @param userInfo
	 * @param webhookId
	 */
	void deleteWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * Verify the Webhook of the corresponding ID by providing the verification code received by invokeEndpoint upon creation/updating. 
	 * After successful verification, Synapse will set isVerified to true.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	VerifyWebhookResponse verifyWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request);
	
	/**
	 * List all webhookIds for a Synapse user. Each call will return a single page of WebhookRegistrations. Forward the provided nextPageToken to get the next page.
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
}
