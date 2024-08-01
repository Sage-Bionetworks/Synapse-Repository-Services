package org.sagebionetworks.repo.manager.webhook;

import java.util.Objects;

/**
 * DTO that is used to publish to the webhook message delivery queue the data to send to the clients
 */
public class WebhookMessage {

	private String webhookId;
	private String endpoint;
	private String messageBody;

	public WebhookMessage() {
	}

	public String getWebhookId() {
		return webhookId;
	}

	public WebhookMessage setWebhookId(String webhookId) {
		this.webhookId = webhookId;
		return this;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public WebhookMessage setEndpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public WebhookMessage setMessageBody(String messageBody) {
		this.messageBody = messageBody;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(endpoint, messageBody, webhookId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof WebhookMessage)) {
			return false;
		}
		WebhookMessage other = (WebhookMessage) obj;
		return Objects.equals(endpoint, other.endpoint) && Objects.equals(messageBody, other.messageBody)
				&& Objects.equals(webhookId, other.webhookId);
	}

	@Override
	public String toString() {
		return "WebhookMessage [webhookId=" + webhookId + ", endpoint=" + endpoint + ", messageBody=" + messageBody + "]";
	}

}
