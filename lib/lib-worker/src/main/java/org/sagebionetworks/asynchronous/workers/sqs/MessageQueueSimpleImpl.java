package org.sagebionetworks.asynchronous.workers.sqs;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;

/**
 * The stripped-bare implementation of a message queue.
 */
public class MessageQueueSimpleImpl implements MessageQueue {

	private static final Logger log = LogManager.getLogger(MessageQueueSimpleImpl.class);

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
		log.info("Queue created. URL: " + queueUrl);
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

	@Override
	public String getDeadLetterQueueName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMaxReceiveCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getDeadLetterQueueUrl() {
		// TODO Auto-generated method stub
		return null;
	}
}
