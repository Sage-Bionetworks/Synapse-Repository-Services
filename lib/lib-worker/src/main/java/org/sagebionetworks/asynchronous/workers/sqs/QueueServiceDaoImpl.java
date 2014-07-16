package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.common.collect.Lists;

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
			if(rmr.getMessages() == null || rmr.getMessages().size() < toFetch){
				// There are no more messages
				break;
			}
			remaining = maxMessages - results.size();
		}
		return results;
	}

	@Override
	public void deleteMessages(String queueUrl, List<Message> messagesToDelete) {
		// Send the data in batches
		Set<String> ids = new HashSet<String>();
		List<DeleteMessageBatchRequestEntry> allEntries = new LinkedList<DeleteMessageBatchRequestEntry>();
		for(Message message: messagesToDelete){
			DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle());
			// Only add messages with new IDs to the batch.
			if(ids.add(message.getMessageId())){
				allEntries.add(entry);
			}
		}
		// Make batches that are the max allowed size for SQS calls.
		List<List<DeleteMessageBatchRequestEntry>> batchedEntries = Lists.partition(allEntries, maxSQSRequestSize);
		for(List<DeleteMessageBatchRequestEntry> batch: batchedEntries){
			amazonSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
		}
	}

	@Override
	public void resetMessageVisibility(String queueUrl, int newVisibiltySeconds, Collection<Message> toReset) {
		List<ChangeMessageVisibilityBatchRequestEntry> allEntries = new LinkedList<ChangeMessageVisibilityBatchRequestEntry>();
		Set<String> ids = new HashSet<String>();
		for(Message message: toReset){
			ChangeMessageVisibilityBatchRequestEntry entry = new ChangeMessageVisibilityBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()).withVisibilityTimeout(newVisibiltySeconds);
			// Only add messages with new IDs to the batch.
			if(ids.add(message.getMessageId())){
				allEntries.add(entry);
			}
		}
		// Make batches that are the max allowed size for SQS calls.
		List<List<ChangeMessageVisibilityBatchRequestEntry>> batchedEntries = Lists.partition(allEntries, maxSQSRequestSize);
		for(List<ChangeMessageVisibilityBatchRequestEntry> batch: batchedEntries){
			amazonSQSClient.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(queueUrl, batch));
		}
	}

}
