package org.sagebionetworks.workers.util.aws.message;


/**
 * Abstraction for a message queue.  Each queue should setup the AWS SQS used by this queue.
 *
 */
public interface MessageQueue extends HasQueueUrl {

	/**
	 * The name of the SQS queue.
	 * @return
	 */
	public String getQueueName();
	
	/**
	 * Is this queue enabled?
	 * @return
	 */
	public boolean isEnabled();

}
