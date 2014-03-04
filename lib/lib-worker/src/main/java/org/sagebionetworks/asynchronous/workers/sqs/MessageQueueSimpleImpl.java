package org.sagebionetworks.asynchronous.workers.sqs;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;

/**
 * The stripped-bare implementation of a message queue.
 */
public class MessageQueueSimpleImpl implements MessageQueue {

	@Autowired
	private AmazonSQSClient awsSQSClient;

	private final String queueName;
	private String queueUrl;

	public MessageQueueSimpleImpl(final String queueName) {
		if (queueName == null) {
			throw new NullPointerException();
		}
		this.queueName = queueName;
	}

	@PostConstruct
	private void init() {
		// Create the queue if it does not already exist
		CreateQueueRequest cqRequest = new CreateQueueRequest(queueName);
		CreateQueueResult cqResult = this.awsSQSClient.createQueue(cqRequest);
		this.queueUrl = cqResult.getQueueUrl();
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
		return true;
	}
}
