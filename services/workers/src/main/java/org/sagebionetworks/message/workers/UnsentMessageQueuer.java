package org.sagebionetworks.message.workers;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

/**
 * Pushes a sequential list of ranges to coordinate workers sending unsent messages.  
 */
public class UnsentMessageQueuer implements Runnable {

	static private Log log = LogFactory.getLog(UnsentMessageQueuer.class);
	
	@Autowired
	private AmazonSQSClient awsSQSClient;
	
	@Autowired
	private MessageQueue unsentMessageQueue;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	private Long approxRangeSize;
	
	public void setApproxRangeSize(long approxRangeSize) {
		if (approxRangeSize <= 0) {
			throw new IllegalArgumentException("Range size must be greater than zero");
		}
		this.approxRangeSize = approxRangeSize;
	}
	
	/**
	 * For testing
	 */
	public void setAwsSQSClient(AmazonSQSClient awsSQSClient) {
		this.awsSQSClient = awsSQSClient;
	}
	
	@Override
	public void run() {
		long count = changeDAO.getCount();
		if (count <= 0) {
			log.info("No change messages");
			return;
		}

		// Only run if the SQS is empty
		if (!isQueueEmpty()) {
			log.info("Queue is not empty");
			return;
		}
		
		List<SendMessageBatchRequestEntry> batch = buildRangeBatch();
		List<List<SendMessageBatchRequestEntry>> miniBatches = MessageUtils.splitListIntoTens(batch);
		for (int i = 0; i < miniBatches.size(); i++) {
			awsSQSClient.sendMessageBatch(new SendMessageBatchRequest(unsentMessageQueue.getQueueUrl(), miniBatches.get(i)));
		}
		log.info("Just queued: " + batch.size() + " message(s)");
	}
	
	/**
	 * Checks to see if the queue is empty
	 */
	protected boolean isQueueEmpty() {
		ReceiveMessageRequest rmRequest = new ReceiveMessageRequest();
		rmRequest.setQueueUrl(unsentMessageQueue.getQueueUrl());
		rmRequest.setVisibilityTimeout(0);
		rmRequest.setWaitTimeSeconds(0);
		rmRequest.setMaxNumberOfMessages(1);
		ReceiveMessageResult rmResult = awsSQSClient.receiveMessage(rmRequest);
		if (rmResult.getMessages().size() <= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Builds up a number of ranges based on the state of the CHANGE table
	 */
	protected List<SendMessageBatchRequestEntry> buildRangeBatch() {
		long count = changeDAO.getCount();
		long min = changeDAO.getMinimumChangeNumber();
		long max = changeDAO.getCurrentChangeNumber();
		long chunks = 1 + count / approxRangeSize;
		long chunkSize = 1 + (max - min) / chunks;
		List<SendMessageBatchRequestEntry> batch = new LinkedList<SendMessageBatchRequestEntry>();
		
		for (int i = 0; i < chunks; i++) {
			addMessageToBatch(batch, i, min + i * chunkSize, min + (i + 1) * chunkSize);
		}
		if (min + chunks * chunkSize < max) {
			addMessageToBatch(batch, chunks, min + chunks * chunkSize, max);
		}
		return batch;
	}
	
	/**
	 * Adds the range to the list of messages to push to SQS
	 */
	private void addMessageToBatch(List<SendMessageBatchRequestEntry> batch, long index, long lower, long upper) {
		if (lower > upper) {
			// This should never be hit since it means the worker's logic is incorrect
			throw new IllegalArgumentException("Upper and lower bounds must have the correct numeric relationship: " + lower + " <= " + upper);
		}
		UnsentMessageRange range = new UnsentMessageRange();
		range.setLowerBound(lower);
		range.setUpperBound(upper);
		SendMessageBatchRequestEntry entry = createEntry(index, range);
		if (entry != null) {
			batch.add(entry);
		}
	}

	/**
	 * Converts a UnsentMessageRange to a AWS-SQS-compatible message
	 */
	private SendMessageBatchRequestEntry createEntry(long index, UnsentMessageRange range) {
		try {
			String messageBody = EntityFactory.createJSONStringForEntity(range);
			return new SendMessageBatchRequestEntry(""+index, messageBody);
		} catch (JSONObjectAdapterException e) {
			log.error("Failed to marshal message", e);
			return null;
		}
	}
}
