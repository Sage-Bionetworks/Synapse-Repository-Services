package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;


/**
 * A manager for servicing Webhooks
 * 
 * @author lmoenning
 */
public interface WebhookManager {
	
	/**
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request);
	
	/**
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @return
	 */
	Webhook getWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * 
	 * @param userInfo
	 * @param webhookId
	 * @param request
	 * @return
	 */
	Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request); 
	
	/**
	 * 
	 * @param userInfo
	 * @param webhookId
	 */
	void deleteWebhook(UserInfo userInfo, String webhookId);
	
	/**
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	VerifyWebhookResponse validateWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request);
	
	/**
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	ListUserWebhooksResponse listUserWebhooks(UserInfo userInfo, ListUserWebhooksRequest request);
	
	/**
	 * 
	 * @param objectId
	 * @return
	 */
	List<Webhook> listSendableWebhooksForObjectId(String objectId);
}
