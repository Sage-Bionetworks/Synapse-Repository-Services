package org.sagebionetworks.repo.model.dbo.dao.webhook;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;

public interface WebhookDao {

	/**
	 * 
	 * @param webhook
	 * @return
	 */
	Webhook createWebhook(Webhook webhook);

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
	Webhook getWebhookForUpdate(String webhookId);

	/**
	 * 
	 * @param webhook
	 * @return
	 */
	Webhook updateWebhook(Webhook webhook);

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
	 * @param objectId
	 * @param webhookObjectType
	 * @return
	 */
	List<Webhook> listVerifiedAndEnabledWebhooksForObjectId(String objectId, ObjectType objectType);

}
