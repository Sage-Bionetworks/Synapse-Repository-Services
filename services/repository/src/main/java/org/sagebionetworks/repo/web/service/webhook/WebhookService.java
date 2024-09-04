package org.sagebionetworks.repo.web.service.webhook;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;

public interface WebhookService {

	Webhook create(Long userId, CreateOrUpdateWebhookRequest request);
	
	Webhook get(Long userId, String webhookId);
	
	ListUserWebhooksResponse list(Long userId, ListUserWebhooksRequest request);
	
	Webhook update(Long userId, String webhookId, CreateOrUpdateWebhookRequest request);
	
	VerifyWebhookResponse verify(Long userId, String webhookId, VerifyWebhookRequest request);
	
	Webhook generateVerificationCode(Long userId, String webhookId);
	
	void delete(Long userId, String webhookId);
	
}
