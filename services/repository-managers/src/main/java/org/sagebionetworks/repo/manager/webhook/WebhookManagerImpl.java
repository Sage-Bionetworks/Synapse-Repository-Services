package org.sagebionetworks.repo.manager.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.trash.TrashCanDao;
import org.sagebionetworks.repo.model.dbo.webhook.DBOWebhookVerification;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookSynapseEventMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Service
public class WebhookManagerImpl implements WebhookManager {	
	
	private static final Logger LOG = LogManager.getLogger(WebhookManagerImpl.class);
	private static final String MESSAGE_QUEUE_NAME = "WEBHOOK_MESSAGE";
	private static final int VERIFICATION_CODE_LENGHT = 6;
	private static final int VERIFICATION_CODE_TTL_SECONDS = 10 * 60;
	private static final long WEBHOOK_FETCH_PAGE_SIZE = 1_000;
	private static final Duration CHANGE_MESSAGE_MAX_AGE = Duration.ofHours(1);
	
	private WebhookDao webhookDao;
	
	private TrashCanDao trashCanDao;
	
	private AmazonSQSClient sqsClient;
	
	private WebhookAuthorizationManager webhookAuthorizationManager;
	
	private NodeDAO nodeDao;
	
	private Clock clock;	
	
	private String queueUrl;
	
	public WebhookManagerImpl(WebhookDao webhookDao, TrashCanDao trashCanDao, AmazonSQSClient sqsClient, WebhookAuthorizationManager webhookAuthorizationManager, NodeDAO nodeDao, Clock clock) {
		this.webhookDao = webhookDao;
		this.trashCanDao = trashCanDao;
		this.sqsClient = sqsClient;
		this.webhookAuthorizationManager = webhookAuthorizationManager;
		this.nodeDao = nodeDao;
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
			verificationMessage = "The verification code has expired.";
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
	@WriteTransaction
	public void updateWebhookVerificationStatus(String webhookId, WebhookVerificationStatus status, String verificationMessage) {
		ValidateArgument.required(webhookId, "The webhookId");
		ValidateArgument.required(status, "The status");
		
		// Lock the webhook
		webhookDao.getWebhook(webhookId, true);
		webhookDao.setWebhookVerificationStatus(webhookId, status, verificationMessage);
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
	public void processChangeMessage(ChangeMessage change) {
		Instant now = clock.now().toInstant();
		
		// Discard old messages
		if (change.getTimestamp().toInstant().plus(CHANGE_MESSAGE_MAX_AGE).isBefore(now)) {
			return;
		}
		
		switch (change.getObjectType()) {
		case ENTITY:
			processEntityChange(SynapseEventType.valueOf(change.getChangeType().name()), change.getTimestamp(), change.getObjectId());
			break;
		default:
			LOG.warn("Unsupported change: " + change);
			break;
		}
		
	}
	
	void processEntityChange(SynapseEventType eventType, Date timestamp, String entityId) {		
		List<Long> pathIds = getEntityActualPathIds(entityId);
		
		if (pathIds.isEmpty()) {
			return;
		}
		
		// Iterator for each webhook subscribed to the entity
		PaginationIterator<Webhook> webhookIterator = new PaginationIterator<>((long limit, long offset) -> 
			webhookDao.listWebhooksForObjectIds(pathIds, SynapseObjectType.ENTITY, eventType, limit, offset)
		, WEBHOOK_FETCH_PAGE_SIZE);
		
		while (webhookIterator.hasNext()) {
			Webhook webhook = webhookIterator.next();
			
			// checks that the user still has permissions on the webhook object
			if (!webhookAuthorizationManager.hasWebhookOwnerReadAccess(webhook)) {
				continue;
			}
			
			publishWebhookMessage(new WebhookSynapseEventMessage()
				.setEventTimestamp(timestamp)
				.setEventType(eventType)
				.setObjectId(entityId)
				.setObjectType(SynapseObjectType.ENTITY)
				.setWebhookId(webhook.getId())
				.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint())
				.setWebhookOwnerId(webhook.getCreatedBy())
			);
		}
	}
	
	List<Long> getEntityActualPathIds(String entityId) {
		Iterator<Long> pathIterator;
		
		try {
			// First gather all the entity ids in the hierarchy
			pathIterator = nodeDao.getEntityPathIds(entityId).iterator();
		} catch (NotFoundException e) {
			// The node does not exists anymore, nothing we can do
			return Collections.emptyList();
		}
		
		// We skip the first id since it is the root node
		pathIterator.next();
		
		if (!pathIterator.hasNext()) {
			return Collections.emptyList();
		}
		
		List<Long> pathIds = new ArrayList<>();
		
		// Fetch the root of the path first
		Long rootId = pathIterator.next();
				
		// If the root of the hierarchy is the trashcan we need to obtain the original path
		if (BOOTSTRAP_NODES.TRASH.getId().equals(rootId)) {
			if (pathIterator.hasNext()) {
				// This is the first node in the path that is in the trashcan
				Long trashedNodeId = pathIterator.next();
				
				trashCanDao.getTrashedEntity(KeyFactory.keyToString(trashedNodeId)).ifPresent(trashedEntity -> {
					List<Long> trashedNodeOriginalPathIds = getEntityActualPathIds(trashedEntity.getOriginalParentId()); 
					pathIds.addAll(trashedNodeOriginalPathIds);
				});

				pathIds.add(trashedNodeId);
			}
		} else {
			pathIds.add(rootId);
		}
		
		// Add the rest of the path
		while (pathIterator.hasNext()) {
			pathIds.add(pathIterator.next());
		}
		
		return pathIds;
	}
	
	void generateAndSendVerificationCode(Webhook webhook) {
		String verificationCode = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGHT);
		Date now = clock.now();
		Instant expiresOn = now.toInstant().plus(VERIFICATION_CODE_TTL_SECONDS, ChronoUnit.SECONDS);
		
		webhookDao.setWebhookVerificationCode(webhook.getId(), verificationCode, expiresOn);
		
		// Note that we publish directly to the message queue as part of the create/update transaction
		// since if this fails we want to rollback
		publishWebhookMessage(new WebhookVerificationMessage()
			.setEventTimestamp(now)
			.setVerificationCode(verificationCode)
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookId(webhook.getId())
			.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint())
		);
	}
	
	void publishWebhookMessage(WebhookMessage event) {
		String messageJson;
		
		try {
			messageJson = EntityFactory.createJSONStringForEntity(event);
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

		webhookAuthorizationManager.getReadAuthorizationStatus(userInfo, request.getObjectType(), request.getObjectId()).checkAuthorizationOrElseThrow();
	}
	
}
