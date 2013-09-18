package org.sagebionetworks.message.workers;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageQueue;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

/**
 * Pushes a sequential list of ranges to coordinate workers sending unsent messages.  
 */
public class UnsentMessageQueuer implements Runnable {

	static private Log log = LogFactory.getLog(UnsentMessageQueuerTest.class);
	
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
	
	@Override
	public void run() {
		long count = changeDAO.getCount();
		if (count <= 0) {
			return;
		}
		
		List<SendMessageBatchRequestEntry> batch = buildRangeBatch();
		awsSQSClient.sendMessageBatch(new SendMessageBatchRequest(unsentMessageQueue.getQueueUrl(), batch));
	}
	
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
	
	private void addMessageToBatch(List<SendMessageBatchRequestEntry> batch, long index, long lower, long upper) {
		if (lower > upper) {
			// This should never be hit since it means the worker's logic is incorrect
			throw new IllegalArgumentException("Upper and lower bounds must have the correct numeric relationship: " + lower + " <= " + upper);
		}
		UnsentMessageRange range = new UnsentMessageRange();
		range.setLowerBound(lower);
		range.setUpperBound(upper);
		SendMessageBatchRequestEntry entry= createEntry(index, range);
		if (entry != null) {
			batch.add(entry);
		}
	}
	
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
