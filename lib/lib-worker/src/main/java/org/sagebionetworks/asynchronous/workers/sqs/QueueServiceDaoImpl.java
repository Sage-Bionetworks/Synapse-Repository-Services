package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQS;
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
	
	private static final int LONG_POLL_WAIT_TIME = 20;
	@Autowired
	AmazonSQS AmazonSQS;
	private int maxSQSRequestSize = 10;
	
	/**
	 * Simple queue services puts limits on the batch size of all operations.
	 * 
	 * @param maxSQSRequestSize
	 */
	public void setMaxSQSRequestSize(int maxSQSRequestSize) {
		this.maxSQSRequestSize = maxSQSRequestSize;
	}

	@Override
	public int getLongPollWaitTimeInSeconds() {
		return LONG_POLL_WAIT_TIME;
	}

	@Override
	public List<Message> receiveMessages(String queueUrl,
			int visibilityTimeoutSec, int maxMessages) {
		List<Message> results = new LinkedList<Message>();
		int remaining = maxMessages;
		while(remaining > 0){
			int toFetch = Math.min(maxSQSRequestSize, remaining);
			ReceiveMessageResult rmr = AmazonSQS.receiveMessage(new ReceiveMessageRequest().withQueueUrl(queueUrl).withVisibilityTimeout(visibilityTimeoutSec).withMaxNumberOfMessages(toFetch));
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
	public List<Message> receiveMessagesLongPoll(String queueUrl, int visibilityTimeoutSec, int maxMessages) {
		List<Message> results = new LinkedList<Message>();
		int remaining = maxMessages;
		boolean firstTime = true;
		while (remaining > 0) {
			int toFetch = Math.min(maxSQSRequestSize, remaining);
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest().withQueueUrl(queueUrl)
					.withVisibilityTimeout(visibilityTimeoutSec).withMaxNumberOfMessages(toFetch);
			if (firstTime) {
				// only for the first request do we want to do a long poll. As soon as we have some messages, we just
				// want to quickly gather the max and then return
				receiveMessageRequest.withWaitTimeSeconds(LONG_POLL_WAIT_TIME);
				firstTime = false;
			}
			ReceiveMessageResult rmr = AmazonSQS.receiveMessage(receiveMessageRequest);
			if (rmr.getMessages() != null) {
				// Add all of the messages to the results.
				results.addAll(rmr.getMessages());
			}
			if (rmr.getMessages() == null || rmr.getMessages().size() < toFetch) {
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
			AmazonSQS.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, batch));
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
			AmazonSQS.changeMessageVisibilityBatch(new ChangeMessageVisibilityBatchRequest(queueUrl, batch));
		}
	}

}
