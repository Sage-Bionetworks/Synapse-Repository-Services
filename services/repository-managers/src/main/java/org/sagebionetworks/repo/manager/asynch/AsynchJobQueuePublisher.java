package org.sagebionetworks.repo.manager.asynch;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;

import com.amazonaws.services.sqs.model.Message;

/**
 * Abstraction for publishing messages.
 * @author jmhill
 *
 */
public interface AsynchJobQueuePublisher {
	
	/**
	 * Publish a job to its queue.
	 * 
	 * @param status
	 */
	public void publishMessage(AsynchronousJobStatus status);
	
	/**
	 * Receive a single message
	 * @param type
	 * @return
	 */
	Message recieveOneMessage(AsynchJobType type);

	/**
	 * Delete a message.
	 * @param type
	 * @param message
	 */
	void deleteMessage(AsynchJobType type, Message message);
	
	/**
	 * Delete all messages from all of the queue
	 */
	void emptyAllQueues();

}
