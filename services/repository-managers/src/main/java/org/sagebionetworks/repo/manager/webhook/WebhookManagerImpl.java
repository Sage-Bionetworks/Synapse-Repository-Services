package org.sagebionetworks.repo.manager.webhook;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.sagebionetworks.repo.model.dbo.webhook.DBOWebhookVerification;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookEvent;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationEvent;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Service
public class WebhookManagerImpl implements WebhookManager {
	
	private static final String MESSAGE_QUEUE_NAME = "WEBHOOK_MESSAGE";
	private static final int VERIFICATION_CODE_LENGHT = 6;
	private static final int VERIFICATION_CODE_TTL_SECONDS = 10 * 60;

	private WebhookDao webhookDao;
	
	private AmazonSQSClient sqsClient;

	private AccessControlListDAO aclDao;
	
	private Clock clock;
	
	private String queueUrl;

	public WebhookManagerImpl(WebhookDao webhookDao, AmazonSQSClient sqsClient, AccessControlListDAO aclDao, Clock clock) {
		this.webhookDao = webhookDao;
		this.sqsClient = sqsClient;
		this.aclDao = aclDao;
		this.clock = clock;
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
	
	Webhook getWebhook(UserInfo userInfo, String webhookId, boolean forUpdate) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.requiredNotBlank(webhookId, "The webhookId");
		
		Webhook webhook = webhookDao.getWebhook(webhookId, forUpdate).orElseThrow(() -> new NotFoundException("A webhook with the given id does not exist."));

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, webhook.getCreatedBy())) {
			throw new UnauthorizedException("You are not authorized to access this resource.");
		}
		
		return webhook;
	}

	@Override
	public Webhook getWebhook(UserInfo userInfo, String webhookId) {
		boolean forUpdate = false;
		return getWebhook(userInfo, webhookId, forUpdate);
	}

	@WriteTransaction
	@Override
	public Webhook updateWebhook(UserInfo userInfo, String webhookId, CreateOrUpdateWebhookRequest request) {
		validateCreateOrUpdateRequest(userInfo, request);
		
		boolean forUpdate = true;
		
		Webhook current = getWebhook(userInfo, webhookId, forUpdate);

		Webhook updated = webhookDao.updateWebhook(webhookId, request);
		
		if (!current.getInvokeEndpoint().equals(updated.getInvokeEndpoint())) {
			generateAndSendVerificationCode(updated);
		}
		
		return updated;
	}

	@WriteTransaction
	@Override
	public void deleteWebhook(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.requiredNotBlank(webhookId, "The webhookId");
		
		boolean forUpdate = true;
		
		Webhook webhook = getWebhook(userInfo, webhookId, forUpdate);
		
		webhookDao.deleteWebhook(webhook.getId());
	}

	@WriteTransaction
	@Override
	public VerifyWebhookResponse verifyWebhook(UserInfo userInfo, String webhookId, VerifyWebhookRequest request) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(webhookId, "The webhookId");
		ValidateArgument.required(request, "The request");
		ValidateArgument.requiredNotBlank(request.getVerificationCode(), "The verificationCode");

		boolean forUpdate = true;
		
		Webhook webhook = getWebhook(userInfo, webhookId, forUpdate);
		
		ValidateArgument.requirement(WebhookVerificationStatus.CODE_SENT.equals(webhook.getVerificationStatus()), "Cannot verify the webhook at this time.");
				
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhookId);
						
		WebhookVerificationStatus newStatus = null;
		String verificationMessage = null;
		
		if (clock.now().after(verification.getCodeExpiresOn())) {
			newStatus = WebhookVerificationStatus.FAILED;
			verificationMessage = "The provided verification code has expired.";
		} else if (request.getVerificationCode().equals(verification.getCode())) {
			newStatus = WebhookVerificationStatus.VERIFIED;
			verificationMessage = null;
		} else {
			newStatus = webhook.getVerificationStatus();
			verificationMessage = "The provided verification code is invalid.";
		}
		
		webhookDao.setWebhookVerificationStatus(webhookId, newStatus, verificationMessage);
		
		return new VerifyWebhookResponse()
			.setIsValid(WebhookVerificationStatus.VERIFIED.equals(newStatus))
			.setInvalidReason(verificationMessage);
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
		// TODO This should be invoked by a worker and a message sent to the endpoint
	}
	
	void generateAndSendVerificationCode(Webhook webhook) {
		String verificationCode = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGHT);
		Date now = clock.now();
		Instant expiresOn = now.toInstant().plus(VERIFICATION_CODE_TTL_SECONDS, ChronoUnit.SECONDS);
		
		webhookDao.setWebhookVerificationCode(webhook.getId(), verificationCode, expiresOn);
		
		// Note that we publish directly to the message queue as part of the create/update transaction
		// since if this fails we want to rollback
		publishWebhookEvent(webhook.getId(), webhook.getInvokeEndpoint(), new WebhookVerificationEvent()
			.setEventId(UUID.randomUUID().toString())
			.setEventTimestamp(now)
			.setVerificationCode(verificationCode)
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookId(webhook.getId())
		);
	}
	
	void publishWebhookEvent(String webhookId, String webhookEndpoint, WebhookEvent event) {
		String messageJson;
		
		try {
		
			WebhookMessage message = new WebhookMessage()
				.setWebhookId(event.getWebhookId())
				.setEndpoint(webhookEndpoint)
				.setIsVerificationMessage(event instanceof WebhookVerificationEvent)
				.setMessageBody(EntityFactory.createJSONStringForEntity(event));
			
			messageJson = EntityFactory.createJSONStringForEntity(message);
		
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		
		sqsClient.sendMessage(queueUrl, messageJson);
	}
	
	void validateCreateOrUpdateRequest(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getObjectId(), "The objectId");
		ValidateArgument.required(request.getObjectType(), "The objectType");
		ValidateArgument.requiredNotEmpty(request.getEventTypes(), "The eventTypes");
		ValidateArgument.validUrl(request.getInvokeEndpoint(), "The invokeEndpoint");
		ValidateArgument.required(request.getIsEnabled(), "isEnabled");
		
		try {
			URI uri = URI.create(request.getInvokeEndpoint());
			URI uriNormalized = new URI("https", uri.getHost(), uri.getPath(), null);
						
			ValidateArgument.requirement(uri.equals(uriNormalized), "The invokedEndpoint only supports https and cannot contain a port, query or fragment");
			
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("The invoke endpoint is invalid.");
		}
		
		AuthorizationUtils.disallowAnonymous(userInfo);

		if (!userInfo.isAdmin()) {
			aclDao.canAccess(userInfo, request.getObjectId(), ObjectType.valueOf(request.getObjectType().name()), ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		}
	}
	
}
