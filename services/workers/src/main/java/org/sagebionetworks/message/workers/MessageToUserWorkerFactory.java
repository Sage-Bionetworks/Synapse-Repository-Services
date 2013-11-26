package org.sagebionetworks.message.workers;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.manager.MessageManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Spawns message sending workers
 */
public class MessageToUserWorkerFactory implements MessageWorkerFactory {
	
	@Autowired
	private MessageManager messageManager;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new MessageToUserWorker(messages, messageManager);
	}

}
