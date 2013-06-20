package org.sagebionetworks.dynamo.workers.sqs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorker implements Callable<List<Message>> {

	private final Logger logger = Logger.getLogger(DynamoQueueWorker.class);

	private final Consumer consumer;

	private final List<Message> messages;

	private final NodeTreeUpdateManager updateManager;

	public DynamoQueueWorker(List<Message> messageList,
			NodeTreeUpdateManager updateManager, Consumer consumer) {

		if (messageList == null) {
			throw new IllegalArgumentException("The list of messages cannot be null.");
		}
		if (updateManager == null) {
			throw new IllegalArgumentException("Update manager cannot be null.");
		}
		if (consumer == null) {
			throw new IllegalArgumentException("Consumer manager cannot be null.");
		}

		this.messages = messageList;
		this.updateManager = updateManager;
		this.consumer = consumer;
	}

	@Override
	public List<Message> call() throws Exception {

		final long start = System.nanoTime();
		final List<Message> processedMessages = new ArrayList<Message>();
		for (Message message : messages) {
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
						updateManager.create(change.getObjectId(),
								change.getParentId(), timestamp);
					} else if (ChangeType.UPDATE.equals(change.getChangeType())) {
						updateManager.update(change.getObjectId(),
								change.getParentId(), timestamp);
					} else if (ChangeType.DELETE.equals(change.getChangeType())) {
						updateManager.delete(change.getObjectId(), timestamp);
					} else {
						throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
					}
					processedMessages.add(message);
				} catch (Throwable e) {
					logger.error("Failed to process message", e);
				}
			} else {
				processedMessages.add(message);
			}
		}

		// Emit a latency metric
		final long latency = (System.nanoTime() - start) / 1000000L;
		ProfileData profileData = new ProfileData();
		profileData.setNamespace("DynamoQueueWorker");
		profileData.setName("TotalLatency"); // Method name
		profileData.setLatency(latency);
		profileData.setUnit("Milliseconds");
		profileData.setTimestamp(new Date());
		consumer.addProfileData(profileData);

		return processedMessages;
	}
}
