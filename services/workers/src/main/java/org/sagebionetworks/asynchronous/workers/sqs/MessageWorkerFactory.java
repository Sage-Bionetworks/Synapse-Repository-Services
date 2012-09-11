package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

/**
 * A Message worker factory provides message workers that process AWS SQS messages.
 * 
 * @author John
 *
 */
public interface MessageWorkerFactory {
	
	/**
	 * Create a new worker to process messages.
	 * @param message 
	 * @return
	 */
	public Callable<Message> createWorker(Message message);

}
