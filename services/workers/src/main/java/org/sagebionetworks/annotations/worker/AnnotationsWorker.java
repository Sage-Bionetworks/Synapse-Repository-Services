package org.sagebionetworks.annotations.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker that processes messages for SubmissionStatus Annotations jobs.
 *
 */
public class AnnotationsWorker implements Callable<List<Message>> {
	
	static private Log log = LogFactory.getLog(AnnotationsWorker.class);
	
	List<Message> messages;
	SubmissionStatusAnnotationsAsyncManager ssAsyncMgr;

	public AnnotationsWorker(List<Message> messages, SubmissionStatusAnnotationsAsyncManager ssAsyncMgr) {
		if(messages == null) throw new IllegalArgumentException("Messages cannot be null");
		if(ssAsyncMgr == null) throw new IllegalArgumentException("Asynchronous DAO cannot be null");
		this.messages = messages;
		this.ssAsyncMgr = ssAsyncMgr;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// process each message
		for (Message message: messages) {
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about Submission messages here
			if (ObjectType.SUBMISSION == change.getObjectType()) {
				try {
					// Is this a create, update, or delete?
					if (ChangeType.CREATE == change.getChangeType()) {
						ssAsyncMgr.createSubmissionStatus(change.getObjectId());
					} else if (ChangeType.UPDATE == change.getChangeType()) {
						// update
						ssAsyncMgr.updateSubmissionStatus(change.getObjectId());
					} else if(ChangeType.DELETE == change.getChangeType()) {
						// delete
						ssAsyncMgr.deleteSubmission(change.getObjectId());
					} else {
						throw new IllegalArgumentException("Unknown change type: "+change.getChangeType());
					}
					// This message was processed.
					processedMessages.add(message);
				} catch (NotFoundException e) {
					log.info("NotFound: "+e.getMessage()+". The message will be returend as processed and removed from the queue");
					// If a Submission does not exist anymore then we want the message to be deleted from the queue
					processedMessages.add(message);
				} catch (JSONObjectAdapterException e) {
					log.info("Parse error: "+e.getMessage()+". The message will be returend as processed and removed from the queue");
					// If a Submission does not exist anymore then we want the message to be deleted from the queue
					processedMessages.add(message);
				} catch (IllegalArgumentException e) {
					log.info("Processing error: "+e.getMessage()+". The message will be returend as processed and removed from the queue");
					// If a Submission does not exist anymore then we want the message to be deleted from the queue
					processedMessages.add(message);
				} catch (Throwable e) {
					// Something went wrong and we did not process the message.
					log.error("Failed to process message", e);
				}
			} else {
				// Non-Submission messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}

}
