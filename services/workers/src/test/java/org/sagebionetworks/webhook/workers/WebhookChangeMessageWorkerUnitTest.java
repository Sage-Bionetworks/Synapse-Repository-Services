package org.sagebionetworks.webhook.workers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.webhook.WebhookManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class WebhookChangeMessageWorkerUnitTest {
	
	@Mock
	private WebhookManager mockManager;
	
	@InjectMocks
	private WebhookChangeMessageWorker worker;
	
	@Mock
	private ChangeMessage mockMessage;

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		// Call under test
		worker.run(null, mockMessage);
		
		verify(mockManager).processChangeMessage(mockMessage);
	}

}
