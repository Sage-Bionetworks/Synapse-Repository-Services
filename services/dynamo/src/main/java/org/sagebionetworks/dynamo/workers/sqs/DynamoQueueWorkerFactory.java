package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorkerFactory implements MessageWorkerFactory{
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new DynamoQueueWorker(messages);
	}
}
