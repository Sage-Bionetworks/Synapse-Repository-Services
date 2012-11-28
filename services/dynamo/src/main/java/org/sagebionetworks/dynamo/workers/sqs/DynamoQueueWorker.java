package org.sagebionetworks.dynamo.workers.sqs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.dynamo.manager.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorker implements Callable<List<Message>> {

	private final Logger logger = Logger.getLogger(DynamoQueueWorker.class);

	private final List<Message> messages;

	private final NodeTreeUpdateManager updateManager;

	public DynamoQueueWorker(List<Message> messageList,
			NodeTreeUpdateManager updateManager) {

		if (messageList == null) {
			throw new NullPointerException();
		}
		if (updateManager == null) {
			throw new NullPointerException();
		}

		this.messages = messageList;
		this.updateManager = updateManager;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new ArrayList<Message>();
		for (Message message : this.messages) {
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			if (ObjectType.ENTITY.equals(change.getObjectType())) {
				Date timestamp = change.getTimestamp();
				if (timestamp == null) {
					// TODO: Not the ideal timestamp.
					// The timestamp should be as close to the source as possible
					timestamp = new Date();
				}
				try {
					if (ChangeType.CREATE.equals(change.getChangeType())) {
						this.updateManager.create(change.getObjectId(),
								change.getParentId(), timestamp);
					} else if (ChangeType.UPDATE.equals(change.getChangeType())) {
						this.updateManager.update(change.getObjectId(),
								change.getParentId(), timestamp);
					} else if (ChangeType.DELETE.equals(change.getChangeType())) {
						this.updateManager.delete(change.getObjectId(), timestamp);
					} else {
						throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
					}
					processedMessages.add(message);
				} catch (Throwable e) {

					System.out.println("Failed to process message");
					do {
						System.out.println(e.getMessage());
						final Writer result = new StringWriter();
						PrintWriter w = new PrintWriter(result);
						e.printStackTrace(w);
						System.out.println(result.toString());
						e = e.getCause();
					} while (e != null);

					this.logger.error("Failed to process message", e);
				}
			} else {
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}
}
