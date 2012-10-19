package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorker implements Callable<List<Message>> {

	private final Logger logger = Logger.getLogger(DynamoQueueWorker.class);

	private final List<Message> messages;

	public DynamoQueueWorker(List<Message> messageList) {
		this.messages = messageList;
	}

	@Override
	public List<Message> call() throws Exception {
		for (Message msg : this.messages) {
			ChangeMessage change = MessageUtils.extractMessageBody(msg);
			this.logger.info(change.toString());
		}
		return this.messages;
	}
}
