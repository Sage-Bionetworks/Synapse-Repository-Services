package org.sagebionetworks.dynamo.workers.sqs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.dynamo.manager.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorker implements Callable<List<Message>> {

	private final Logger logger = Logger.getLogger(DynamoQueueWorker.class);

	private final List<Message> messages;

	@Autowired
	private NodeTreeUpdateManager nodeTreeManager;

	public DynamoQueueWorker(List<Message> messageList) {
		this.messages = messageList;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new ArrayList<Message>();
		for(Message message : this.messages){
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			if (ObjectType.ENTITY.equals(change.getObjectType())) {
				try {
					if (ChangeType.CREATE.equals(change.getChangeType())) {
						this.nodeTreeManager.create(change.getObjectId(),
								change.getParentId(), change.getTimestamp());
					} else if (ChangeType.UPDATE.equals(change.getChangeType())) {
						this.nodeTreeManager.update(change.getObjectId(),
								change.getParentId(), change.getTimestamp());
					} else if (ChangeType.DELETE.equals(change.getChangeType())) {
						this.nodeTreeManager.delete(change.getObjectId(), change.getTimestamp());
					} else {
						throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
					}
					processedMessages.add(message);
				} catch (Throwable e) {
					this.logger.error("Failed to process message", e);
				}
			} else {
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}
}
