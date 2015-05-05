package org.sagebionetworks.asynchronous.workers.sqs;


/**
 * Abstraction for a message queue.  Each queue should setup the AWS SQS used by this queue.
 * @author John
 *
 */
public interface MessageQueue {

	/**
	 * The name of the SQS queue.
	 * @return
	 */
	public String getQueueName();

	/**
	 * The URL of the AWS SQS queue.
	 * @return
	 */
	public String getQueueUrl();
	
	/**
	 * Is this queue enabled?
	 * @return
	 */
	public boolean isEnabled();
	
	/**
	 * The name of the dead letter queue
	 * @return
	 */
	public String getDeadLetterQueueName();
	
	/**
	 * The URL of the dead letter queue associated with this queue
	 * @return
	 */
	public String getDeadLetterQueueUrl();
	
	/**
	 * The number of times a msg is received before moving to the dead letter queue
	 * @return
	 */
	public int getMaxReceiveCount();

}
