package org.sagebionetworks.webhook.workers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.webhook.WebhookMessageDispatcher;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class WebhookMessageWorkerUnitTest {

	@Mock
	private WebhookMessageDispatcher dispatcher;
	
	@InjectMocks
	private WebhookMessageWorker worker;
	
	@Mock
	private WebhookMessage mockMessage;
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		// Call under test
		worker.run(null, null, mockMessage);
		
		verify(dispatcher).dispatchMessage(mockMessage);
	}

}
