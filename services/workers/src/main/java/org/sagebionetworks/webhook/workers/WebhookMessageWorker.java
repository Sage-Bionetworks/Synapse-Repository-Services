package org.sagebionetworks.webhook.workers;

import java.util.List;

import org.sagebionetworks.repo.manager.webhook.WebhookMessageDispatcher;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class WebhookMessageWorker implements MessageDrivenRunner {

	private static final List<String> ATTR_NAMES = List.of("All");
	
	private WebhookMessageDispatcher dispatcher;
	
	public WebhookMessageWorker(WebhookMessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		dispatcher.dispatchMessage(message);
	}
	
	@Override
	public List<String> getMessageAttributeNames() {
		return ATTR_NAMES;
	}

}
