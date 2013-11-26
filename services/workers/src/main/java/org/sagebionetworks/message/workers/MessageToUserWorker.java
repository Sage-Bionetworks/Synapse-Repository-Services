package org.sagebionetworks.message.workers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker that processes messages sending messages to users
 * 
 */
public class MessageToUserWorker implements Callable<List<Message>> {

	static private Logger log = LogManager.getLogger(MessageToUserWorker.class);

	private List<Message> messages;
	private MessageManager messageManager;

	public MessageToUserWorker(List<Message> messages,
			MessageManager messageManager) {
		if (messages == null) {
			throw new IllegalArgumentException("Messages cannot be null");
		}
		if (messageManager == null) {
			throw new IllegalArgumentException("MessageManager cannot be null");
		}
		this.messages = messages;
		this.messageManager = messageManager;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		for (Message message : messages) {
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			
			// We only care about MESSAGE messages here
			if (ObjectType.MESSAGE == change.getObjectType()) {
				try {
					List<String> errors = null;
					switch (change.getChangeType()) {
					case CREATE:
						errors = messageManager.sendMessage(change.getObjectId());
						break;
					default:
						throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
					}
					
					//TODO How should we handle error messages?  Relay them to the user?
					if (errors.size() > 0) {
						log.info("Errors while processing message ("
								+ change.getObjectId() + ")\n"
								+ StringUtils.join(errors, "\n"));
					}
					
					// This message was processed
					processedMessages.add(message);
				} catch (NotFoundException e) {
					log.info("NotFound: " + e.getMessage() + ". The message will be returned as processed and removed from the queue");
					processedMessages.add(message);
				} catch (Throwable e) {
					// Something went wrong and we did not process the message
					log.error("Failed to process message", e);
				}
			} else {
				// Non-MESSAGE messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}

}
