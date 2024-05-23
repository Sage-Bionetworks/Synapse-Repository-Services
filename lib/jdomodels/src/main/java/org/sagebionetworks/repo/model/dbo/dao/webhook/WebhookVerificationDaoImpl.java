package org.sagebionetworks.repo.model.dbo.dao.webhook;

import org.sagebionetworks.repo.model.webhook.WebhookVerification;

public class WebhookVerificationDaoImpl implements WebhookVerificationDao {

	@Override
	public WebhookVerification createWebhookVerification(WebhookVerification webhookVerification) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WebhookVerification getWebhookVerification(String webhookId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long incrementAttempts(String webhookId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void pruneExpiredWebhookVerifications() {
		// TODO Auto-generated method stub

	}

}
