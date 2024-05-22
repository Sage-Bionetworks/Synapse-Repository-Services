package org.sagebionetworks.repo.model.dbo.dao.webhook;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;

public class WebhookDaoImpl implements WebhookDao {

	@Override
	public Webhook createWebhook(Webhook webhook) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Webhook getWebhook(String webhookId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Webhook getWebhookForUpdate(String webhookId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Webhook updateWebhook(Webhook webhook) {
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
	public List<Webhook> listVerifiedAndEnabledWebhooksForObjectId(String objectId, ObjectType objectType) {
		// TODO Auto-generated method stub
		return null;
	}

}
