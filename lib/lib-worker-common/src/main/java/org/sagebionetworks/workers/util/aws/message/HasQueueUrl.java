package org.sagebionetworks.workers.util.aws.message;

/**
 * Abstraction for a provider of a queue URL.
 *
 */
public interface HasQueueUrl {

	/**
	 * The URL of the AWS SQS queue.
	 * @return
	 */
	public String getQueueUrl();
}
