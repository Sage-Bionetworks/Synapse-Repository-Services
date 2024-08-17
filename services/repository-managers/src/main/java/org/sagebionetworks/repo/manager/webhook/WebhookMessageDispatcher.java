package org.sagebionetworks.repo.manager.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
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
	
	static final String HEADER_WEBHOOK_ID = "X-Synapse-WebhookId";
	static final String HEADER_WEBHOOK_OWNER_ID = "X-Synapse-WebhookOwnerId";
	static final String HEADER_WEBHOOK_MESSAGE_TYPE = "X-Synapse-WebhookMessageType";
	
	static final BodyHandler<Void> DISCARDING_BODY_HANDLER = BodyHandlers.discarding();
	
	static final EnumSet<HttpStatus> ACCEPTED_HTTP_STATUS = EnumSet.of(
		HttpStatus.OK, 
		HttpStatus.ACCEPTED, 
		HttpStatus.CREATED, 
		HttpStatus.NO_CONTENT
	);
	
	private WebhookManager webhookManager;
	private HttpClient webhookHttpClient;
	private String userAgent;
	
	public WebhookMessageDispatcher(WebhookManager webhookManager, HttpClient webhookHttpClient) {
		this.webhookManager = webhookManager;
		this.webhookHttpClient = webhookHttpClient;
	}
	
	@Autowired
	public void configure(StackConfiguration config) {
		userAgent = "Synapse-Webhook/" + config.getStackInstance();
	}
	
	public void dispatchMessage(Message message) throws URISyntaxException {
		Map<String, MessageAttributeValue> messageAttributes = message.getMessageAttributes();
		
		String webhookId = messageAttributes.get(WebhookManager.MSG_ATTR_WEBHOOK_ID).getStringValue();
		String webhookOwnerId = messageAttributes.get(WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID).getStringValue();
		String webhookEndpoint = messageAttributes.get(WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT).getStringValue();
		String messageTypeString = messageAttributes.get(WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE).getStringValue();
		
		WebhookMessageType messageType = WebhookMessageType.valueOf(messageTypeString);
		
		HttpRequest request = HttpRequest.newBuilder(new URI(webhookEndpoint))
			.timeout(REQUEST_TIMEOUT)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.USER_AGENT, userAgent)
			.header(HEADER_WEBHOOK_ID, webhookId)
			.header(HEADER_WEBHOOK_OWNER_ID, webhookOwnerId)
			.header(HEADER_WEBHOOK_MESSAGE_TYPE, messageTypeString)
			.POST(BodyPublishers.ofString(message.getBody()))
			.build();
		
		webhookHttpClient.sendAsync(request, DISCARDING_BODY_HANDLER)
			.whenComplete((response, exception) -> handleResponse(messageType, webhookId, response, exception));
	}
	
	void handleResponse(WebhookMessageType messageType, String webhookId, HttpResponse<Void> response, Throwable exception) {
		// Since we discard the response, we only care about the status code if any
		HttpStatus httpStatus = response != null ? HttpStatus.resolve(response.statusCode()) : null;
		
		switch (messageType) {
		case Verification:
			handleWebhookVerificationResponse(webhookId, httpStatus, exception);
			break;
		case SynapseEvent:
			handleWebhookSynapseEventResponse(webhookId, httpStatus, exception);
			break;
		default:
			LOG.warn("Unhandled response for message type " + messageType);
			break;
		}
	}
	
	void handleWebhookVerificationResponse(String webhookId, HttpStatus status, Throwable exception) {
		WebhookVerificationStatus newStatus = null;
		String verificationMessage = null;
		
		if (exception != null) {
			LOG.error("Webhook {} verification request failed with exception: ", webhookId, exception);
			newStatus = WebhookVerificationStatus.FAILED;
			verificationMessage = "The request to the webhook endpoint failed.";
		} else if (status == null) {			
			newStatus = WebhookVerificationStatus.FAILED;
			verificationMessage = "The request to the webhook endpoint failed with no response.";
		} else if (!ACCEPTED_HTTP_STATUS.contains(status)) {
			newStatus = WebhookVerificationStatus.FAILED;
			verificationMessage = "The request to the webhook endpoint failed with status " + status.value() + ".";
		} else {
			newStatus = WebhookVerificationStatus.CODE_SENT;
			verificationMessage = "A verification code was sent to the webhook endpoint.";
		}
		
		webhookManager.updateWebhookVerificationStatus(webhookId, newStatus, verificationMessage);
	}
	
	void handleWebhookSynapseEventResponse(String webhookId, HttpStatus status, Throwable exception) {
		// TODO Keep track of the failures and eventually revoke the verification
	}
	

}
