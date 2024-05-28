package org.sagebionetworks.workers.util.aws.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * A helper to purge all messages from an AWS SQS queue.
 * 
 */
public class QueueCleaner {
	
	private static final Logger log = LogManager
			.getLogger(QueueCleaner.class);
	
	AmazonSQSClient amazonSQSClient;

	public QueueCleaner(AmazonSQSClient amazonSQSClient) {
		super();
		this.amazonSQSClient = amazonSQSClient;
	}

	
	/**
	 * Purge all messages from the queue with the given name.
	 * 
	 * Will do nothing if the queue does not exist.
	 * 
	 * @param queueName
	 */
	public void purgeQueue(String queueName){
		String messageQueueUrl = null;
		try {
			messageQueueUrl = this.amazonSQSClient.getQueueUrl(queueName).getQueueUrl();
		} catch (QueueDoesNotExistException e) {
			log.info("Queue: "+queueName+" does not exists");
			return;
		}
		
		/*
		 * It would be nice to use {@link
		 * com.amazonaws.services.sqs.AmazonSQSClient
		 * #purgeQueue(PurgeQueueRequest)} however, Amazon only allows it to be
		 * called every 60 seconds so it cannot be used for test that need to
		 * start with an empty queue.  Therefore, we simply pull and delete messages.
		 */
		while(true){
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
			receiveMessageRequest.setMaxNumberOfMessages(10);
			receiveMessageRequest.setWaitTimeSeconds(0);
			receiveMessageRequest.setQueueUrl(messageQueueUrl);
			ReceiveMessageResult results = this.amazonSQSClient.receiveMessage(receiveMessageRequest);
			deleteMessageBatch(messageQueueUrl, results.getMessages());
			if(results.getMessages().isEmpty()){
				//stop when there are no more messages.
				break;
			}
		}
	}
	
	/**
	 * Delete a batch of messages.
	 * @param batch
	 */
	private void deleteMessageBatch(String messageQueueUrl, List<Message> batch){
		if(batch != null){
			if(!batch.isEmpty()){
				List<DeleteMessageBatchRequestEntry> entryList = new LinkedList<DeleteMessageBatchRequestEntry>();
				for(Message message: batch){
					entryList.add(new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()));
				}
				amazonSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(messageQueueUrl, entryList));
			}
		}
	}

}
