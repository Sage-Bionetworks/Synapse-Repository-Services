package org.sagebionetworks.repo.manager.webhook;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

@Service
public class WebhookMessageDispatcher {

	private static final Logger LOG = LogManager.getLogger(WebhookMessageDispatcher.class);

	public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

	private static final int TOKEN_EXPIRATION_SECONDS = 30;
	
	private static final String HEADER_PREFIX = "X-Syn-Webhook-";
	static final String HEADER_WEBHOOK_ID = HEADER_PREFIX + "Id";
	static final String HEADER_WEBHOOK_MSG_ID = HEADER_PREFIX + "Message-Id";
	static final String HEADER_WEBHOOK_OWNER_ID = HEADER_PREFIX + "Owner-Id";
	static final String HEADER_WEBHOOK_MESSAGE_TYPE = HEADER_PREFIX + "Message-Type";

	static final BodyHandler<Void> DISCARDING_BODY_HANDLER = BodyHandlers.discarding();

	static final EnumSet<HttpStatus> ACCEPTED_HTTP_STATUS = EnumSet.of(
		HttpStatus.OK, 
		HttpStatus.ACCEPTED, 
		HttpStatus.CREATED,
		HttpStatus.NO_CONTENT
	);

	static final EnumSet<HttpStatus> RETRY_HTTP_STATUS = EnumSet.of(
		HttpStatus.TOO_MANY_REQUESTS, 
		HttpStatus.INTERNAL_SERVER_ERROR,
		HttpStatus.BAD_GATEWAY, 
		HttpStatus.SERVICE_UNAVAILABLE, 
		HttpStatus.GATEWAY_TIMEOUT
	);

	private WebhookManager manager;
	private OIDCTokenManager tokenManager;
	private WebhookMetricsCollector metricsCollector;
	private HttpClient webhookHttpClient;
	private Clock clock;

	// Configured after construction
	private String userAgent;
	private String tokenIssuer;

	public WebhookMessageDispatcher(WebhookManager manager, OIDCTokenManager tokenManager, WebhookMetricsCollector metricsCollector, @Qualifier("webhookHttpClient") HttpClient webhookHttpClient, Clock clock) {
		this.manager = manager;
		this.tokenManager = tokenManager;
		this.metricsCollector = metricsCollector;
		this.webhookHttpClient = webhookHttpClient;
		this.clock = clock;
	}

	@Autowired
	public void configure(StackConfiguration config) {
		this.userAgent = "Synapse-Webhook/" + config.getStackInstance();
		this.tokenIssuer = "https://repo-prod." + config.getStack() + ".sagebase.org/auth/v1";
	}

	public void dispatchMessage(Message message) {
		WebhookMessageAttributes attributes = new WebhookMessageAttributes(message.getMessageAttributes());
		
		if (attributes.isVerification()) {
			// We need to make sure that the current committed verification matches the messageId and that the webhook exists
			WebhookVerificationStatus status = manager.getWebhookVerificationStatus(attributes.getWebhookId(), attributes.getMessageId()).orElse(null);
						
			// Only PENDING and FAILED (in case of retry) verification can be processed by this worker
			if (!WebhookVerificationStatus.PENDING.equals(status) && !WebhookVerificationStatus.FAILED.equals(status)) {
				LOG.warn("Invalid verification message (WebhookId: {}, MessageId: {}, Status: {}, Endpoint: {})", attributes.getWebhookId(), attributes.getMessageId(), status, attributes.getWebhookEndpoint());
				throw new RecoverableMessageException();
			}
		}
		
		String messageToken = tokenManager.createWebhookMessageToken(tokenIssuer, attributes.getMessageId(), message.getMD5OfBody(), attributes.getWebhookOwnerId(), TOKEN_EXPIRATION_SECONDS);
		
		HttpRequest request = HttpRequest.newBuilder(URI.create(attributes.getWebhookEndpoint()))
			.timeout(REQUEST_TIMEOUT)
			.header(HttpHeaders.AUTHORIZATION, AuthorizationConstants.BEARER_TOKEN_HEADER + messageToken)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.USER_AGENT, userAgent)
			.headers(attributes.toRequestHeaders())
			.POST(BodyPublishers.ofString(message.getBody()))
			.build();
		
		sendWebhookRequest(attributes, request);
				
	}
	
	void sendWebhookRequest(WebhookMessageAttributes attributes, HttpRequest request) {
		long start = clock.currentTimeMillis();
		
		CompletableFuture<HttpResponse<Void>> asyncResponse = webhookHttpClient.sendAsync(request, DISCARDING_BODY_HANDLER);
		
		final HttpResponse<Void> response;
		
		try {
			// We wait for the response so that the worker controls the level of concurrency to avoid exhausting resources
			response = asyncResponse.get(REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
		} catch (Throwable ex) {
			LOG.warn("The {} request (WebhookId: {}, MessageId: {}, Endpoint: {}) failed exceptionally:", attributes.getMessageType(), attributes.getWebhookId(), attributes.getMessageId(), attributes.getWebhookEndpoint(), ex);
			
			metricsCollector.requestCompleted(attributes.getWebhookId(), clock.currentTimeMillis() - start, true);
			
			updateVerificationStatus(attributes, false, null, ex);
			
			throw new RecoverableMessageException(ex);
		} 

		HttpStatus status = HttpStatus.resolve(response.statusCode());
		
		if (ACCEPTED_HTTP_STATUS.contains(status)) {
			metricsCollector.requestCompleted(attributes.getWebhookId(), clock.currentTimeMillis() - start, false);
			updateVerificationStatus(attributes, true, response, null);
			return;
		}
		
		LOG.warn("The {} request (WebhookId: {}, MessageId: {}, Endpoint: {}) failed with status: {}.", attributes.getMessageType(), attributes.getWebhookId(), attributes.getMessageId(), attributes.getWebhookEndpoint(), response.statusCode());

		metricsCollector.requestCompleted(attributes.getWebhookId(), clock.currentTimeMillis() - start, true);
		
		updateVerificationStatus(attributes, false, response, null);
		
		if (RETRY_HTTP_STATUS.contains(status)) {
			throw new RecoverableMessageException();
		}
	}
	
	void updateVerificationStatus(WebhookMessageAttributes attributes, boolean success, HttpResponse<Void> response, Throwable ex) {
		
		if (!attributes.isVerification()) {
			return;
		}
		
		WebhookVerificationStatus newStatus;
		String newMessage;
		
		if (success) {
			newStatus = WebhookVerificationStatus.CODE_SENT;
			newMessage = "A code was sent to the webhook endpoint.";
		} else {
			newStatus = WebhookVerificationStatus.FAILED;
			StringBuilder messageBuilder = new StringBuilder("The request to the webhook endpoint failed");

			if (response != null) {
				messageBuilder.append(" with status ").append(response.statusCode()).append(".");
			} else if (ex != null) {
				Throwable cause = ex;
				if (ex instanceof ExecutionException) {
					cause = ex.getCause();
				}
				
				if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException) {
					messageBuilder.append(" (Reason: connection timeout).");
				} else if (cause instanceof InterruptedException || cause instanceof TimeoutException || cause instanceof HttpTimeoutException) {
					messageBuilder.append(" (Reason: request timeout).");
				} else {
					messageBuilder.append(" (Reason: unknown).");
				}
			} else {
				messageBuilder.append(" (Reason: unknown).");
			}
			newMessage = messageBuilder.toString();			
		}
		
		manager.updateWebhookVerificationStatus(attributes.getWebhookId(), attributes.getMessageId(), newStatus, newMessage);
	}

	static final class WebhookMessageAttributes {
		
		private final String messageId;
		private final String webhookId;
		private final WebhookMessageType messageType;
		private final String webhookEndpoint;
		private final String webhookOwnerId;
		private final boolean isVerification;
		
		static String getMessageAttribute(Map<String, MessageAttributeValue> messageAttributes, String attributeName) {
			MessageAttributeValue value = messageAttributes.get(attributeName);
			
			if (value == null || value.getStringValue() == null) {
				throw new IllegalStateException("Could not find attribute: " + attributeName);
			}
			
			return value.getStringValue();
		}

		protected WebhookMessageAttributes(Map<String, MessageAttributeValue> messageAttributes) {
			this.messageId = getMessageAttribute(messageAttributes, WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID);
			this.webhookId = getMessageAttribute(messageAttributes, WebhookManager.MSG_ATTR_WEBHOOK_ID);
			this.webhookOwnerId = getMessageAttribute(messageAttributes, WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID);
			this.webhookEndpoint = getMessageAttribute(messageAttributes, WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT);
			this.messageType = WebhookMessageType.valueOf(getMessageAttribute(messageAttributes, WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE));
			this.isVerification = WebhookMessageType.Verification.equals(this.messageType);
		}

		public String getMessageId() {
			return messageId;
		}

		String getWebhookId() {
			return webhookId;
		}

		WebhookMessageType getMessageType() {
			return messageType;
		}

		String getWebhookEndpoint() {
			return webhookEndpoint;
		}

		String getWebhookOwnerId() {
			return webhookOwnerId;
		}

		boolean isVerification() {
			return isVerification;
		}
		
		String[] toRequestHeaders() {
			return new String[] {
				HEADER_WEBHOOK_ID, this.webhookId,
				HEADER_WEBHOOK_MSG_ID, this.messageId,
				HEADER_WEBHOOK_OWNER_ID, this.webhookOwnerId,
				HEADER_WEBHOOK_MESSAGE_TYPE, this.messageType.name()
			};
		}

		@Override
		public int hashCode() {
			return Objects.hash(isVerification, messageId, messageType, webhookEndpoint, webhookId, webhookOwnerId);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof WebhookMessageAttributes)) {
				return false;
			}
			WebhookMessageAttributes other = (WebhookMessageAttributes) obj;
			return isVerification == other.isVerification && Objects.equals(messageId, other.messageId) && messageType == other.messageType
					&& Objects.equals(webhookEndpoint, other.webhookEndpoint) && Objects.equals(webhookId, other.webhookId)
					&& Objects.equals(webhookOwnerId, other.webhookOwnerId);
		}

	}

}
