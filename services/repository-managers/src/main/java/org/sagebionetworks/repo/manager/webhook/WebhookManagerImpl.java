package org.sagebionetworks.repo.manager.webhook;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookEvent;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationEvent;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

@Service
public class WebhookManagerImpl implements WebhookManager {
	
	private static final String MESSAGE_QUEUE_NAME = "WEBHOOK_MESSAGE";
	private static final int VERIFICATION_CODE_LENGHT = 6;
	private static final int VERIFICATION_CODE_TTL_SECONDS = 10 * 60;

	private WebhookDao webhookDao;
	
	private AmazonSQSClient sqsClient;

	private AccessControlListDAO aclDao;
	
	private String queueUrl;

	public WebhookManagerImpl(WebhookDao webhookDao, AmazonSQSClient sqsClient, AccessControlListDAO aclDao) {
		this.webhookDao = webhookDao;
		this.sqsClient = sqsClient;
		this.aclDao = aclDao;
	}
	
	@Autowired
	public void configureMessageQueueUrl(StackConfiguration config) {
		 queueUrl = sqsClient.getQueueUrl(config.getQueueName(MESSAGE_QUEUE_NAME)).getQueueUrl();
	}

	@WriteTransaction
	@Override
	public Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		validateCreateOrUpdateRequest(userInfo, request);

		Webhook webhook = webhookDao.createWebhook(userInfo.getId(), request);
		
		generateAndSendVerificationCode(webhook);
		
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
			generateAndSendVerificationCode(updated);
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
		// TODO 
	}
	
	void generateAndSendVerificationCode(Webhook webhook) {
		String verificationCode = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGHT);
		Instant now = Instant.now();
		Instant expiresOn = now.plus(VERIFICATION_CODE_TTL_SECONDS, ChronoUnit.SECONDS);
		
		webhookDao.setVerificationCode(webhook.getId(), verificationCode, expiresOn);
		
		publishWebhookEvent(webhook.getId(), webhook.getInvokeEndpoint(), new WebhookVerificationEvent()
			.setEventId(UUID.randomUUID().toString())
			.setEventTimestamp(Date.from(now))
			.setVerificationCode(verificationCode)
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookId(webhook.getId())
		);
	}
	
	void publishWebhookEvent(String webhookId, String webhookEndpoint, WebhookEvent event) {
		String messageJson;
		
		try {
		
			WebhookMessage message = new WebhookMessage()
				.setWebhookId(webhookId)
				.setEndpoint(webhookEndpoint)
				.setIsVerificationMessage(event instanceof WebhookVerificationEvent)
				.setMessageBody(EntityFactory.createJSONStringForEntity(event));
			
			messageJson = EntityFactory.createJSONStringForEntity(message);
		
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		
		sqsClient.sendMessage(new SendMessageRequest(queueUrl, messageJson));
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
