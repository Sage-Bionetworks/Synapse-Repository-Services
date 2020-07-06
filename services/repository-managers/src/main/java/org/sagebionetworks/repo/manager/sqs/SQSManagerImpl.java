package org.sagebionetworks.repo.manager.sqs;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.SQSSendMessageRequest;
import org.sagebionetworks.repo.model.message.SQSSendMessageResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageResult;

@Service
public class SQSManagerImpl implements SQSManager {

	private StackConfiguration config;
	private AmazonSQS sqsClient;
	
	@Autowired
	public SQSManagerImpl(final StackConfiguration config, final AmazonSQS sqsClient) {
		this.config = config;
		this.sqsClient = sqsClient;
	}
	
	@Override
	public SQSSendMessageResponse sendMessage(final SQSSendMessageRequest messageRequest) throws NotFoundException, TemporarilyUnavailableException {
		ValidateArgument.required(messageRequest, "messageRequest");
		ValidateArgument.requiredNotBlank(messageRequest.getRelativeQueueName(), "messageRequest.relativeQueueName");
		ValidateArgument.requiredNotBlank(messageRequest.getMessageBody(), "messageRequest.messageBody");

		final String queueName = messageRequest.getRelativeQueueName();
		final String messageBody = messageRequest.getMessageBody();
		
		final SendMessageResult result;
		
		try {
			final String stackQueueUrl = getStackQueueUrl(queueName);
			result = sqsClient.sendMessage(stackQueueUrl, messageBody);
		} catch (QueueDoesNotExistException ex) {
			throw new NotFoundException("The queue referenced by " + queueName + " does not exist", ex);
		} catch (AmazonSQSException ex) {
			// This is a service exception and we cannot recover from this, AWS cannot process the request
			throw new IllegalArgumentException(ex.getMessage(), ex);
		} catch (AmazonClientException ex) {
			throw new TemporarilyUnavailableException("Could not send SQS message to queue " + queueName + ": " + ex.getMessage(), ex);
		}
		
		SQSSendMessageResponse response = new SQSSendMessageResponse();
		
		response.setMessageId(result.getMessageId());
		
		return response;
	}
	
	/**
	 * @return The url of the queue with the given name, the name is normalized to the stack
	 */
	String getStackQueueUrl(final String queueName) {
		final String stackQueueName = getStackQueueName(queueName);
		return sqsClient.getQueueUrl(stackQueueName).getQueueUrl();
	}

	/**
	 * @return The stack normalized queue name
	 */
	String getStackQueueName(final String queueName) {
		return config.getQueueName(queueName);
	}

}
