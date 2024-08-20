package org.sagebionetworks.webhook.workers;

import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.webhook.WebhookManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

@Service
public class WebhookChangeMessageWorker implements ChangeMessageDrivenRunner {
	
	private WebhookManager manager;
	
	public WebhookChangeMessageWorker(WebhookManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message) throws RecoverableMessageException, Exception {
		manager.processChangeMessage(message);
	}

}
