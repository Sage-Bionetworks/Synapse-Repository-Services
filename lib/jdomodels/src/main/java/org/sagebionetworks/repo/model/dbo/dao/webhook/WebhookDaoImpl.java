package org.sagebionetworks.repo.model.dbo.dao.webhook;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;

public class WebhookDaoImpl implements WebhookDao {

	@Override
	public Webhook createWebhook(Long userId, CreateOrUpdateWebhookRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Webhook getWebhook(String webhookId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<String> getWebhookOwnerForUpdate(String webhookId) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Webhook updateWebhook(Long userId, String webhookId, CreateOrUpdateWebhookRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteWebhook(String webhookId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Webhook> listUserWebhooks(Long userId, long limit, long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Webhook> listVerifiedAndEnabledWebhooksForObjectId(String objectId, WebhookObjectType webhookObjectType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWebhookVerificationStatus(String webhookId, boolean verificationStatus) {
		// TODO Auto-generated method stub
		
	}

}
