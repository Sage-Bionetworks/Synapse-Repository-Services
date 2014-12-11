package org.sagebionetworks.asynchronous.workers.sqs;

import com.amazonaws.services.sqs.model.Message;

/**
 * Abstraction used to communicate that progress is being make on message.
 * 
 * @author jmhill
 *
 */
public interface WorkerProgress {
	
	/**
	 * Report that progress has been made on a message.
	 * 
	 * Any worker that can run longer than the visibility timeout of a message, must call this method regularly
	 * to ensure the message is not returned to the queue.  The parent process will extend the visibility timeout
	 * of the message if needed when this method is called.
	 * 
	 * A worker can keep a message invisible for up to nine hours as long a progress continues to be made.
	 * 
	 * 
	 * @param messag
	 */
	public void progressMadeForMessage(Message message);

	/**
	 * Report that this message must be retried after a timeout
	 * 
	 * A worker can delay rehandling a message between 1 second and 9 hours After calling this method, the handler
	 * Should stop handling this message since another handler could pick up the message and not return the message from
	 * the handler call (which would cause it to be deleted)
	 * 
	 * @param message
	 */
	public void retryMessage(Message message, int retryTimeoutInSeconds);
}
