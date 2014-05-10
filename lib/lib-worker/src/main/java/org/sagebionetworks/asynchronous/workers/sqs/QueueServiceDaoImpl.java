package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * Batching implementation of the QueueServiceDao.
 * 
 * @author jmhill
 *
 */
public class QueueServiceDaoImpl implements QueueServiceDao {
	
	@Autowired
	AmazonSQSClient amazonSQSClient;
	private int maxSQSRequestSize = 10;
	
	/**
	 * Simple queue services puts limits on the batch size of all opperations.
	 * 
	 * @param maxSQSRequestSize
	 */
	public void setMaxSQSRequestSize(int maxSQSRequestSize) {
		this.maxSQSRequestSize = maxSQSRequestSize;
	}

	@Override
	public List<Message> receiveMessages(String queueUrl,
			int visibilityTimeoutSec, int maxMessages) {
		List<Message> results = new LinkedList<Message>();
		int remaining = maxMessages;
		while(remaining > 0){
			int toFetch = Math.min(maxSQSRequestSize, remaining);
			ReceiveMessageResult rmr = amazonSQSClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueUrl).withVisibilityTimeout(visibilityTimeoutSec).withMaxNumberOfMessages(toFetch));
			if(rmr.getMessages() != null){
				// Add all of the messages to the results.
				results.addAll(rmr.getMessages());
			}
			remaining = maxMessages - results.size();
		}
		return results;
	}

	@Override
	public void deleteMessages(String queueUrl, List<Message> messagesToDelete) {
		// Send the data in batches
		List<DeleteMessageBatchRequestEntry> batch = new LinkedList<DeleteMessageBatchRequestEntry>();
		for(Message message: messagesToDelete){
			DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle());
			batch.add(entry);
			if(batch.size() == maxSQSRequestSize){
				amazonSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
				batch.clear();
			}
		}
		if(batch.size() > 0){
			amazonSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
		}
	}

	@Override
	public void resetMessageVisibility(String queueUrl, int newVisibiltySeconds, Collection<Message> toReset) {
		List<ChangeMessageVisibilityBatchRequestEntry> batch = new LinkedList<ChangeMessageVisibilityBatchRequestEntry>();
		for(Message message: toReset){
			ChangeMessageVisibilityBatchRequestEntry entry = new ChangeMessageVisibilityBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()).withVisibilityTimeout(newVisibiltySeconds);
			batch.add(entry);
			if(batch.size() == maxSQSRequestSize){
				amazonSQSClient.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(queueUrl, batch));
				batch.clear();
			}
		}
		if(batch.size() > 0){
			amazonSQSClient.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(queueUrl, batch));
		}
	}

}
