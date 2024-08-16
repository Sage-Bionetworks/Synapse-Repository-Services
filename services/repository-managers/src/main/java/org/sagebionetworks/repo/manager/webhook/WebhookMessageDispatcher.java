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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class WebhookMessageDispatcher {
	
	private static final Logger LOG = LogManager.getLogger(WebhookMessageDispatcher.class);
	
	public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
	
	private static final String HEADER_WEBHOOK_ID = "X-Synapse-WebhookId";
	private static final String HEADER_WEBHOOK_OWNER_ID = "X-Synapse-WebhookOwnerId";
	private static final String HEADER_WEBHOOK_MESSAGE_TYPE = "X-Synapse-WebhookMessageType";
	private static final BodyHandler<Void> DISCARDING_BODY_HANDLER = BodyHandlers.discarding();
	
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
	
	public void dispatchMessage(WebhookMessage message) {
		HttpRequest request;
		
		Class<? extends WebhookMessage> messageType = message.getClass();
		
		try {
			request = HttpRequest.newBuilder(new URI(message.getWebhookInvokeEndpoint()))
				.timeout(REQUEST_TIMEOUT)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.USER_AGENT, userAgent)
				.header(HEADER_WEBHOOK_ID, message.getWebhookId())
				.header(HEADER_WEBHOOK_OWNER_ID, message.getWebhookOwnerId())
				.header(HEADER_WEBHOOK_MESSAGE_TYPE, messageType.getSimpleName())
				.POST(BodyPublishers.ofString(EntityFactory.createJSONStringForEntity(message)))
				.build();
		} catch (URISyntaxException | JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
		
		webhookHttpClient.sendAsync(request, DISCARDING_BODY_HANDLER)
			.whenComplete((response, exception) -> handleResponse(messageType, message.getWebhookId(), response, exception));
	}
	
	void handleResponse(Class<? extends WebhookMessage> messageType, String webhookId, HttpResponse<Void> response, Throwable exception) {
		// Since we discard the response, we only care about the status code if any
		HttpStatus httpStatus = response != null ? HttpStatus.resolve(response.statusCode()) : null;
		
		if (WebhookVerificationMessage.class.equals(messageType)) {
			handleWebhookVerificationResponse(webhookId, httpStatus, exception);
		} else {
			handleWebhookSynapseChangeResponse(webhookId, httpStatus, exception);
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
		} else if (!status.is2xxSuccessful()) {
			newStatus = WebhookVerificationStatus.FAILED;
			verificationMessage = "The request to the webhook endpoint failed with status " + status.value();
		} else {
			newStatus = WebhookVerificationStatus.CODE_SENT;
			verificationMessage = "A verification code was sent to the webhook endpoint.";
		}
		
		webhookManager.updateWebhookVerificationStatus(webhookId, newStatus, verificationMessage);
	}
	
	void handleWebhookSynapseChangeResponse(String webhookId, HttpStatus status, Throwable exception) {
		// TODO Keep track of the failures and eventually revoke the verification
	}
	

}
