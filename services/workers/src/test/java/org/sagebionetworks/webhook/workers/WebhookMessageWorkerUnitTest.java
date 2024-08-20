package org.sagebionetworks.webhook.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.webhook.WebhookMessageDispatcher;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class WebhookMessageWorkerUnitTest {

	@Mock
	private WebhookMessageDispatcher dispatcher;
	
	@InjectMocks
	private WebhookMessageWorker worker;
	
	@Mock
	private Message mockMessage;
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		// Call under test
		worker.run(null, mockMessage);
		
		verify(dispatcher).dispatchMessage(mockMessage);
	}
	
	@Test
	public void testGetMessageAttributeNames() {
		assertEquals(List.of("All"), worker.getMessageAttributeNames());
	}

}
