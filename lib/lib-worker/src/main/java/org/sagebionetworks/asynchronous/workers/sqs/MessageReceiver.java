package org.sagebionetworks.asynchronous.workers.sqs;

/**
 * Abstraction for a message receiver.  The message receiver's job is to fetch messages from a queue and hand
 * them off to a worker.  If the worker processes the message successfully the message will be deleted from the queue.
 * 
 * @author John
 *
 */
public interface MessageReceiver extends Runnable {
	
	/**
	 * This is where the MessageReceiver will do all work.  A timer must be setup to fire this method.
	 * @throws InterruptedException 
	 */
	public int triggerFired() throws InterruptedException;

}
