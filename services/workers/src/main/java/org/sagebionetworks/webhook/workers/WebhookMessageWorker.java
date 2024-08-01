package org.sagebionetworks.webhook.workers;

import org.sagebionetworks.repo.manager.webhook.WebhookManager;
import org.sagebionetworks.repo.manager.webhook.WebhookMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class WebhookMessageWorker implements TypedMessageDrivenRunner<WebhookMessage> {

	private WebhookManager manager;
	
	public WebhookMessageWorker(WebhookManager manager) {
		this.manager = manager;
	}

	@Override
	public Class<WebhookMessage> getObjectClass() {
		return WebhookMessage.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, WebhookMessage convertedMessage) throws RecoverableMessageException, Exception {
		manager.processWebhookMessage(convertedMessage);		
	}

}
