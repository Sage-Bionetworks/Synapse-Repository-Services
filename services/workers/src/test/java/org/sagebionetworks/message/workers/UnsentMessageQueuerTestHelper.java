package org.sagebionetworks.message.workers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.UnsentMessageRange;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class UnsentMessageQueuerTestHelper {
	
	@Autowired
	private AmazonSQSClient awsSQSClient;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	public static final long UNSENT_MESSAGE_QUEUER_TEST_SEED = 5330L;
	private static Random random;
	
	public void initialize() {
		random = new Random(UNSENT_MESSAGE_QUEUER_TEST_SEED);
	}
	
	/**
	 * Helper to empty a queue of UnsentMessageRange objects
	 */
	public List<UnsentMessageRange> emptyQueue(String queueURL) {
		// Fetch as many messages as possible and don't wait
		ReceiveMessageRequest rmRequest = new ReceiveMessageRequest(queueURL);
		rmRequest.setWaitTimeSeconds(0);
		rmRequest.setMaxNumberOfMessages(10);
		
		List<Message> messages = new ArrayList<Message>();
		List<DeleteMessageBatchRequestEntry> batch = new ArrayList<DeleteMessageBatchRequestEntry>();
		List<UnsentMessageRange> ranges = new ArrayList<UnsentMessageRange>();
		do {
			ReceiveMessageResult rmResult = awsSQSClient.receiveMessage(rmRequest); 
			messages = rmResult.getMessages();
			
			// Transform messages into delete requests
			for (int i = 0; i < messages.size(); i++) {
				Message message = messages.get(i);
				ranges.add(MessageUtils.extractUnsentMessageBody(message));
				DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle());
				batch.add(entry);
			}
		} while (messages.size() > 0);
		
		if (batch.size() > 0) {
			DeleteMessageBatchRequest dmbRequest = new DeleteMessageBatchRequest(queueURL, batch);
			awsSQSClient.deleteMessageBatch(dmbRequest);
		}
		return ranges;
	}

	/**
	 * @param batch ChangeMessages that have been inserted/replaced into the CHANGES table
	 * @return Set of change numbers that should fall between the minimum and maximum of the CHANGE_NUM column
	 */
	public Set<Long> convertBatchToRange(List<ChangeMessage> batch) {
		long minChange = changeDAO.getMinimumChangeNumber();
		long maxChange = changeDAO.getCurrentChangeNumber();
		
		Set<Long> changeNumbers = new HashSet<Long>();
		for (int i = 0; i < batch.size(); i++) {
			Long number = batch.get(i).getChangeNumber();
			if (number >= minChange && number <= maxChange) {
				changeNumbers.add(number);
			}
		}
		return changeNumbers;
	}

	/**
	 * Helper to build up a list of changes
	 * Based on similarly named method found in DBOChangeDAOImplAutowiredTest
	 * 
	 * Randomly and uniformly distributes change number IDs 
	 * between the given upper and lower bound (inclusive)
	 * 
	 * A smaller bound interval results in sparser change numbers
	 * A larger bound interval results in denser change numbers
	 */
	public List<ChangeMessage> createList(int numChangesInBatch, ObjectType type, long lowerBound, long upperBound) {
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>();
		for(int i=0; i<numChangesInBatch; i++){
			ChangeMessage change = new ChangeMessage();
			long changeNum = lowerBound + (random.nextLong() % (upperBound - lowerBound + 1));
			if(ObjectType.ENTITY == type){
				change.setObjectId("syn" + changeNum);
			}else{
				change.setObjectId("" + changeNum);
			}
			change.setObjectEtag("etag" + changeNum);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(type);
			batch.add(change);
		}
		return batch;
	}
}
