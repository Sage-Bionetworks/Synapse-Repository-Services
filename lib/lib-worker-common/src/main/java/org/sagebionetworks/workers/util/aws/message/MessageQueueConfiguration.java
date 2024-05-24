package org.sagebionetworks.workers.util.aws.message;

/**
 * Configuration information used to configure an AWS SQS queue with additional
 * parameters for registering the queue to receive message from an AWS SNS topic
 * 
 */
public class MessageQueueConfiguration {

	String queueName;
	boolean isEnabled = true;

	public MessageQueueConfiguration() {
	}

	/**
	 * The name of queue.
	 * 
	 * @param queueName
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Is this queue enabled?
	 * 
	 * @param isEnabled
	 */
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * @see {@link MessageQueueConfiguration#setQueueName(String)}
	 * @return
	 */
	public String getQueueName() {
		return queueName;
	}

	/**
	 * @see {@link MessageQueueConfiguration#isEnabled()}
	 * @return
	 */
	public boolean isEnabled() {
		return isEnabled;
	}

}
