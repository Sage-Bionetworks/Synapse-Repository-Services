package org.sagebionetworks.repo.manager.webhook;

import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class WebhookManagerImpl implements WebhookManager {

	@Autowired
	private WebhookDao webhookDao;

	@Autowired
	private AccessControlListDAO aclDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private Clock clock;

	@Autowired
	private TransactionalMessenger transactionalMessenger;

	@WriteTransaction
	@Override
	public Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		validateCreateOrUpdateRequest(userInfo, request);

		Webhook webhook = webhookDao.createWebhook(userInfo.getId(), request);
		
		// TODO enqueue a message to perform endpoint validation
		
		return webhook;
	}

	@Override
	public Webhook getWebhook(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "The userInfo");
		
		Webhook webhook = getWebhookOrThrow(webhookId);

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, webhook.getCreatedBy())) {
			throw new UnauthorizedException("You are not authorized to access this resource.");
		}
		
		return webhook;
	}

	@WriteTransaction
	@Override
	public Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request) {
		validateCreateOrUpdateRequest(userInfo, request);
		
		Webhook current = getWebhook(userInfo, webhookId);

		Webhook updated = webhookDao.updateWebhook(webhookId, request);
		
		if (!current.getInvokeEndpoint().equals(updated.getInvokeEndpoint())) {
			// TODO set PENDING verification status and enqueue a message to perform endpoint validation
		}
		
		return updated;
	}

	@WriteTransaction
	@Override
	public void deleteWebhook(UserInfo userInfo, String webhookId) {
		webhookDao.deleteWebhook(getWebhook(userInfo, webhookId).getId());
	}

	@WriteTransaction
	@Override
	public Webhook verifyWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request) {
		
		return getWebhookOrThrow(webhookId);
	}

	@Override
	public ListUserWebhooksResponse listUserWebhooks(UserInfo userInfo, ListUserWebhooksRequest request) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(request, "The request");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		
		List<Webhook> page = webhookDao.listUserWebhooks(userInfo.getId(), nextPageToken.getLimitForQuery(), nextPageToken.getOffset());

		return new ListUserWebhooksResponse()
			.setPage(page)
			.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}
	
	@Override
	public void processWebhookMessage(WebhookMessage message) {
		// TODO Auto-generated method stub
		
	}

	void validateCreateOrUpdateRequest(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getObjectId(), "The objectId");
		ValidateArgument.required(request.getObjectType(), "The objectType");
		ValidateArgument.requiredNotEmpty(request.getEventTypes(), "The eventTypes");
		ValidateArgument.requiredNotBlank(request.getInvokeEndpoint(), "The invokeEndpoint");
		ValidateArgument.required(request.getIsEnabled(), "isEnabled");
		
		// TODO endpoint URL validation

		AuthorizationUtils.disallowAnonymous(userInfo);

		if (!userInfo.isAdmin()) {
			aclDao.canAccess(userInfo, request.getObjectId(), ObjectType.valueOf(request.getObjectType().name()), ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		}
	}
	
	private Webhook getWebhookOrThrow(String webhookId) {
		ValidateArgument.required(webhookId, "The webhookId");
		return webhookDao.getWebhook(webhookId).orElseThrow(() -> new NotFoundException("A webhook with the given id does not exist."));
	}

}
