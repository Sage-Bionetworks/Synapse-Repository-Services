package org.sagebionetworks.repo.manager.webhook;

import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookSynapseEventMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage;

public enum WebhookMessageType {
	Verification(WebhookVerificationMessage.class),
	SynapseEvent(WebhookSynapseEventMessage.class);
	
	private Class<? extends WebhookMessage> messageClass;
	
	WebhookMessageType(Class<? extends WebhookMessage> messageClass) {
		this.messageClass = messageClass;
	}
	
	public Class<? extends WebhookMessage> getMessageClass() {
		return messageClass;
	}
	
	public static WebhookMessageType forClass(Class<? extends WebhookMessage> messageClass) {
		for (WebhookMessageType type : values()) {
			if (type.getMessageClass().equals(messageClass)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported class " + messageClass);
	}
	
}
