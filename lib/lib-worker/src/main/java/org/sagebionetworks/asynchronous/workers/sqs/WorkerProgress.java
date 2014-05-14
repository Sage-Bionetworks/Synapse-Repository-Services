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

}
