package org.sagebionetworks.repo.manager.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.sagebionetworks.repo.web.WebhookDomainUnsupportedException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service
public class WebhookManagerImpl implements WebhookManager {	
	
	private static final Logger LOG = LogManager.getLogger(WebhookManagerImpl.class);
	private static final String MESSAGE_QUEUE_NAME = "WEBHOOK_MESSAGE";
	private static final int VERIFICATION_CODE_LENGHT = 6;
	private static final int VERIFICATION_CODE_TTL_SECONDS = 10 * 60;
	private static final long WEBHOOK_FETCH_PAGE_SIZE = 1_000;
	private static final Duration CHANGE_MESSAGE_MAX_AGE = Duration.ofHours(1);
	private static final List<Pattern> DEFAULT_ALLOWED_DOMAINS = List.of(
		compileDomainPattern("^.+\\.execute-api\\..+\\.amazonaws\\.com$").get()
	);
	static final Duration DOMAIN_CACHE_EXPIRATION = Duration.ofMinutes(5);
	
	static Optional<Pattern> compileDomainPattern(String regex) {
		try {
			return Optional.of(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
		} catch (PatternSyntaxException e) {
			LOG.warn("Skipping malformed domain pattern {}.", regex, e);
			return Optional.empty();
		}
	}
	
	static Map<String, MessageAttributeValue> mapMessageAttributes(Class<? extends WebhookMessage> messageClass, Webhook webhook, String messageId) {
		return Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID, new MessageAttributeValue().withDataType("String").withStringValue(messageId),
			WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT, new MessageAttributeValue().withDataType("String").withStringValue(webhook.getInvokeEndpoint()),
			WebhookManager.MSG_ATTR_WEBHOOK_ID, new MessageAttributeValue().withDataType("String").withStringValue(webhook.getId()),
			WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID, new MessageAttributeValue().withDataType("String").withStringValue(webhook.getCreatedBy()),
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE, new MessageAttributeValue().withDataType("String").withStringValue(WebhookMessageType.forClass(messageClass).name())
		);
	}
	
	private WebhookDao webhookDao;

	private NodeManager nodeManager;
	
	private AmazonSQSClient sqsClient;
	
	private WebhookAuthorizationManager webhookAuthorizationManager;
		
	private Clock clock;
	
	private String queueUrl;
	
	private LoadingCache<Boolean, List<Pattern>> allowedDomainPatterns;
	
	public WebhookManagerImpl(WebhookDao webhookDao, NodeManager nodeManager, AmazonSQSClient sqsClient, WebhookAuthorizationManager webhookAuthorizationManager, Clock clock) {
		this.nodeManager = nodeManager;
		this.webhookDao = webhookDao;
		this.sqsClient = sqsClient;
		this.webhookAuthorizationManager = webhookAuthorizationManager;
		this.clock = clock;
		this.allowedDomainPatterns = CacheBuilder.newBuilder()
			.ticker(new Ticker() {
				@Override
				public long read() {
					return clock.nanoTime();
				}
			})
			.expireAfterWrite(DOMAIN_CACHE_EXPIRATION)
			.build(CacheLoader.from(this::loadAllowedDomainPatterns));
	}
	
	@Autowired
	public void configureMessageQueueUrl(StackConfiguration config) {
		 queueUrl = sqsClient.getQueueUrl(config.getQueueName(MESSAGE_QUEUE_NAME)).getQueueUrl();
	}
	
	List<Pattern> loadAllowedDomainPatterns() {
		return Stream.concat(
			DEFAULT_ALLOWED_DOMAINS.stream(), 
			webhookDao.getAllowedDomainsPatterns().stream()
				.map(WebhookManagerImpl::compileDomainPattern)
				.flatMap(Optional::stream)
		).collect(Collectors.toList());		
	}

	@WriteTransaction
	@Override
	public Webhook createWebhook(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		validateCreateOrUpdateRequest(userInfo, request);

		Webhook webhook = webhookDao.createWebhook(userInfo.getId(), request);
		
		return generateAndSendVerificationCode(userInfo, webhook);
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
			return generateAndSendVerificationCode(userInfo, updated);
		}
		
		return updated;
	}
	
	@WriteTransaction
	@Override
	public Webhook sendNewVerficationCode(UserInfo userInfo, String webhookId) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(webhookId, "The webhookId");
		
		boolean forUpdate = true;
		
		Webhook webhook = getWebhook(userInfo, webhookId, forUpdate);

		ValidateArgument.requirement(!WebhookVerificationStatus.VERIFIED.equals(webhook.getVerificationStatus()), "The webhook is already verified.");
		
		return generateAndSendVerificationCode(userInfo, webhook);
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
				
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhook.getId());
						
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
		
		webhookDao.setWebhookVerificationStatus(webhook.getId(), newStatus, verificationMessage);
		
		return new VerifyWebhookResponse()
			.setIsValid(WebhookVerificationStatus.VERIFIED.equals(newStatus))
			.setInvalidReason(verificationMessage);
	}
	
	@Override
	public Optional<WebhookVerificationStatus> getWebhookVerificationStatus(String webhookId, String messageId) {
		ValidateArgument.required(webhookId, "The webhookId");
		ValidateArgument.required(messageId, "The messageId");
		return webhookDao.getWebhookVerificationStatus(webhookId, messageId);
	}
	
	@Override
	@WriteTransaction
	public void updateWebhookVerificationStatus(String webhookId, String messageId, WebhookVerificationStatus status, String verificationMessage) {
		ValidateArgument.required(webhookId, "The webhookId");
		ValidateArgument.required(status, "The status");
		ValidateArgument.required(messageId, "The messageId");
		
		// Lock the webhook
		if (webhookDao.getWebhook(webhookId, true).isEmpty()) {
			return;
		}
		
		webhookDao.setWebhookVerificationStatusIfMessageIdMatch(webhookId, messageId, status, verificationMessage);
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
	
	Webhook getWebhook(UserInfo userInfo, String webhookId, boolean forUpdate) {
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.requiredNotBlank(webhookId, "The webhookId");
				
		Webhook webhook = webhookDao.getWebhook(webhookId, forUpdate).orElseThrow(() -> new NotFoundException("A webhook with the given id does not exist."));

		if (!AuthorizationUtils.isUserCreatorOrAdmin(userInfo, webhook.getCreatedBy())) {
			throw new UnauthorizedException("You are not authorized to access this resource.");
		}
		
		return webhook;
	}
	
	void processEntityChange(SynapseEventType eventType, Date timestamp, String entityId) {		
		List<Long> pathIds = nodeManager.getEntityActualPathIds(entityId);
		
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
			
			publishWebhookMessage(webhook, new WebhookSynapseEventMessage()
				.setEventTimestamp(timestamp)
				.setEventType(eventType)
				.setObjectId(entityId)
				.setObjectType(SynapseObjectType.ENTITY),
				UUID.randomUUID().toString()
			);
		}
	}
	
	Webhook generateAndSendVerificationCode(UserInfo userInfo, Webhook webhook) {
		String verificationCode = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGHT);
		Date now = clock.now();
		Instant expiresOn = now.toInstant().plus(VERIFICATION_CODE_TTL_SECONDS, ChronoUnit.SECONDS);
		
		String messageId = webhookDao.setWebhookVerificationCode(webhook.getId(), verificationCode, expiresOn).getCodeMessageId();
		
		Webhook updatedWebhook = getWebhook(userInfo, webhook.getId());
		
		// Note that we publish directly to the message queue as part of the create/update transaction
		// since if this fails we want to rollback.
		// There is a chance that the message is received prior to the transaction being committed, in such
		// case the worker wont find a status with the matching messageId and will retry
		publishWebhookMessage(webhook, new WebhookVerificationMessage()
			.setEventTimestamp(now)
			.setVerificationCode(verificationCode), 
			messageId
		);		
		
		return updatedWebhook;
	}
	
	void publishWebhookMessage(Webhook webhook, WebhookMessage event, String messageId) {
		String messageJson;
		
		try {
			messageJson = EntityFactory.createJSONStringForEntity(event);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		
		sqsClient.sendMessage(
			new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(messageJson)
				.withMessageAttributes(mapMessageAttributes(event.getClass(), webhook, messageId))
		);
	}
	
	void validateCreateOrUpdateRequest(UserInfo userInfo, CreateOrUpdateWebhookRequest request) {
		
		ValidateArgument.required(userInfo, "The userInfo");
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getObjectId(), "The objectId");
		ValidateArgument.required(request.getObjectType(), "The objectType");
		ValidateArgument.requiredNotEmpty(request.getEventTypes(), "The eventTypes");
		ValidateArgument.validUrl(request.getInvokeEndpoint(), "The invokeEndpoint");
		ValidateArgument.required(request.getIsEnabled(), "isEnabled");

		AuthorizationUtils.disallowAnonymous(userInfo);
		
		if (SynapseObjectType.ENTITY.equals(request.getObjectType()) && BOOTSTRAP_NODES.getAllBootstrapIds().contains(KeyFactory.stringToKey(request.getObjectId()))) {
			throw new IllegalArgumentException("The specified object is not valid.");
		}
		
		URI uri = URI.create(request.getInvokeEndpoint());
		
		try {
			
			URI uriNormalized = new URI("https", uri.getHost(), uri.getPath(), null);
						
			ValidateArgument.requirement(uri.equals(uriNormalized), "The invokedEndpoint only supports https and cannot contain a port, query or fragment");
			
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("The invoke endpoint is invalid.");
		}
		
		allowedDomainPatterns.getUnchecked(true).stream()
			.filter(pattern -> pattern.matcher(uri.getHost()).matches())
			.findFirst()
			.orElseThrow(() -> new WebhookDomainUnsupportedException());

		webhookAuthorizationManager.getReadAuthorizationStatus(userInfo, request.getObjectType(), request.getObjectId()).checkAuthorizationOrElseThrow();
	}
	
	
}
