package org.sagebionetworks.repo.service.webhook;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.webhook.WebhookManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.springframework.stereotype.Service;

@Service
public class WebhookServiceImpl implements WebhookService {

	private final WebhookManager webhookManager;
	private final UserManager userManager;
	
	public WebhookServiceImpl(WebhookManager webhookManager, UserManager userManager) {
		this.webhookManager = webhookManager;
		this.userManager = userManager;
	}

	@Override
	public Webhook create(Long userId, CreateOrUpdateWebhookRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.createWebhook(userInfo, request);
	}

	@Override
	public Webhook get(Long userId, String webhookId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.getWebhook(userInfo, webhookId);
	}

	@Override
	public ListUserWebhooksResponse list(Long userId, ListUserWebhooksRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.listUserWebhooks(userInfo, request);
	}

	@Override
	public Webhook update(Long userId, String webhookId, CreateOrUpdateWebhookRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.updateWebhook(userInfo, webhookId, request);
	}

	@Override
	public VerifyWebhookResponse verify(Long userId, String webhookId, VerifyWebhookRequest request) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.verifyWebhook(userInfo, webhookId, request);
	}

	@Override
	public Webhook generateVerificationCode(Long userId, String webhookId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return webhookManager.generateWebhookVerificationCode(userInfo, webhookId);
	}

	@Override
	public void delete(Long userId, String webhookId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		webhookManager.deleteWebhook(userInfo, webhookId);
	}

}
