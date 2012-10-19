package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorker implements Callable<List<Message>> {

	private final List<Message> messages;

	public DynamoQueueWorker(List<Message> messageList) {
		this.messages = messageList;
	}

	@Override
	public List<Message> call() throws Exception {
		return this.messages;
	}
}
