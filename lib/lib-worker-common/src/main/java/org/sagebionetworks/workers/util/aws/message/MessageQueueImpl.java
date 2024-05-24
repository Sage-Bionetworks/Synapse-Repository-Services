package org.sagebionetworks.workers.util.aws.message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

/**
 * Provides information about an AWS SQS queue. The constructor will create a
 * queue with the configured name if the queue does not already exist. The ARN
 * and URL of the queue will also be cached for the queue.
 * 
 * If the provided configuration information includes a dead letter queue name
 * and max failure count, then queue will be configured with the dead letter
 * queue.
 * 
 * Additionally, if the configuration includes a list of AWS SNS topic names the
 * queue will be configured to receive messages from each topic. This includes
 * creating each topic if it does not exist, configuring the policy of the queue
 * such that the topic has permission to push messages to the queue and subscribing the
 * queue to the topic.
 */
public class MessageQueueImpl implements MessageQueue {

	public static final String PROTOCOL_SQS = "sqs";

	public static final String QUEUE_ARN_KEY = "QueueArn";

	private Logger logger = LogManager.getLogger(MessageQueueImpl.class);

	// The first argument is the ARN of the queue, and the second is the ARN of the topic.
	public static final String GRAN_SET_MESSAGE_TEMPLATE = "{ \"Id\":\"GrantRepoTopicSendMessage\", \"Statement\": [{ \"Sid\":\"1\",  \"Resource\": \"%1$s\", \"Effect\": \"Allow\", \"Action\": \"SQS:SendMessage\", \"Condition\": {\"ArnEquals\": {\"aws:SourceArn\": %2$s}}, \"Principal\": {\"AWS\": \"*\"}}]}";

	private AmazonSQSClient awsSQSClient;

	private final String queueName;
	private String queueUrl;
	private boolean isEnabled;

	/**
	 * @param awsSQSClient An AmazonSQSClient configured with credentials.
	 * @param config Configuration information for this queue.
	 */
	public MessageQueueImpl(AmazonSQSClient awsSQSClient,
							MessageQueueConfiguration config) {
		this.awsSQSClient = awsSQSClient;
		this.isEnabled = config.isEnabled();
		this.queueName = config.getQueueName();
		if (this.queueName == null) {
			throw new IllegalArgumentException("QueueName cannot be null");
		}
		setup();
	}

	private void setup() {
		// Do nothing if it is not enabled
		if(!isEnabled){
			logger.info("Queue: "+queueName+" will not be configured because it is not enabled");
			return;
		}

		GetQueueUrlResult result = awsSQSClient.getQueueUrl(queueName);
		this.queueUrl = result.getQueueUrl();
	}

	@Override
	public String getQueueUrl() {
		return this.queueUrl;
	}

	@Override
	public String getQueueName(){
		return this.queueName;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

}
