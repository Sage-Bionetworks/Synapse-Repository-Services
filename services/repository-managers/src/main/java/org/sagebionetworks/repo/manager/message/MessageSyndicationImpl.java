package org.sagebionetworks.repo.manager.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;

/**
 * Basic implementation of the message syndication.
 * 
 * @author jmhill
 *
 */
public class MessageSyndicationImpl implements MessageSyndication {
	
	static private Log log = LogFactory.getLog(RepositoryMessagePublisherImpl.class);
	
	@Autowired
	RepositoryMessagePublisher messagePublisher;
	
	@Autowired
	private AmazonSQSClient awsSQSClient;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Override
	public void rebroadcastAllChangeMessages() {
		// List all change messages
		List<ChangeMessage> list = null;
		long lastNumber = 0;
		do{
			list = changeDAO.listChanges(lastNumber, ObjectType.ENTITY, 100);
			log.info("Sending "+list.size()+" change messages to the topic");
			if(list.size() > 0){
				log.info("First change number on the list: "+list.get(0).getChangeNumber());
			}
			// Add each message
			for(ChangeMessage change: list){
				messagePublisher.fireChangeMessage(change);
				lastNumber = change.getChangeNumber()+1;
			}
		}while(list.size() > 0);
		
	}

	/**
	 * This can be called to run against a give stack by passing all the stack information.
	 * @param args
	 */
	public static void main(String[] args){
		// Create the spring context
		ClassPathXmlApplicationContext beanContext = new ClassPathXmlApplicationContext("classpath:managers-spb.xml");
		
		// Get the MessageSyndication bean
		MessageSyndication messageSyndication = beanContext.getBean(MessageSyndication.class);
		// Rebroadcast all messages
		messageSyndication.rebroadcastAllChangeMessages();
	}

	@Override
	public long rebroadcastChangeMessagesToQueue(String queueName, ObjectType type, Long startChangeNumber, Long limit) {
		// Look up the URL fo the Queue
		String queUrl = lookupQueueURL(queueName);
		// List all change messages
		List<ChangeMessage> list = null;
		long count = 0;
		long lastNumber = startChangeNumber;
		do{
			long localLimit = Math.min(10, limit);
			list = changeDAO.listChanges(lastNumber, type, localLimit);
			log.info("Sending "+list.size()+" change messages to the queue: "+queueName);
			if(list.size() > 0){
				log.info("First change number on the list: "+list.get(0).getChangeNumber());
				// Add each message
				List<SendMessageBatchRequestEntry> batch = new LinkedList<SendMessageBatchRequestEntry>();
				for(int i=0; i<list.size(); i++){
					ChangeMessage change = list.get(i);
					lastNumber = change.getChangeNumber()+1;
					SendMessageBatchRequestEntry entry = createEntry(change, i);
					if(entry != null){
						batch.add(entry);
					}
				}
				// Send this batch
				SendMessageBatchResult results = awsSQSClient.sendMessageBatch(new SendMessageBatchRequest(queUrl, batch));
				// If we failed to send any messages then try again
				List<BatchResultErrorEntry> failed = results.getFailed();
				if(failed!= null && failed.size() > 0){
					log.info("Failed to send "+failed.size()+" messages, these will be sent again");
					// Prepare a new batch.
					batch = prepareFailedBatch(list, failed);
					// Send the second results
					results = awsSQSClient.sendMessageBatch(new SendMessageBatchRequest(queUrl, batch));
					if(failed!= null && failed.size() > 0){
						log.error("Failed to send "+failed.size()+" messages after a second try.  The failed messages will not be sent again");
						for(BatchResultErrorEntry error: failed){
							int index = Integer.parseInt(error.getId());
							ChangeMessage change = list.get(index);
							log.error("Failed to send change message: "+change.getChangeNumber()+" after two attempts.");
						}
					}
				}
			}
			count += list.size();
		}while(list.size() > 0 && count <= limit);
		// return the count
		return count;
	}

	/**
	 * Prepare a batch of failed messages to be resent.
	 * @param list
	 * @param failed
	 * @return
	 */
	private List<SendMessageBatchRequestEntry> prepareFailedBatch(List<ChangeMessage> list, List<BatchResultErrorEntry> failed) {
		List<SendMessageBatchRequestEntry> batch;
		batch = new LinkedList<SendMessageBatchRequestEntry>();
		for(BatchResultErrorEntry error: failed){
			int index = Integer.parseInt(error.getId());
			ChangeMessage change = list.get(index);
			SendMessageBatchRequestEntry entry = createEntry(change, index);
			if(entry != null){
				batch.add(entry);
			}
		}
		return batch;
	}
	
	/**
	 * Create an entry from a message.
	 * @param change
	 * @param index
	 * @return
	 */
	private SendMessageBatchRequestEntry createEntry(ChangeMessage change, int index){
		try {
			String messageBody = EntityFactory.createJSONStringForEntity(change);
			return new SendMessageBatchRequestEntry(""+index, messageBody);
		} catch (JSONObjectAdapterException e) {
			log.error("Failed to marshal message", e);
			return null;
		}
	}

	/**
	 * Lookup a queue URL given the queue name.
	 * @param queueName
	 * @return
	 */
	public String lookupQueueURL(String queueName) {
		ListQueuesResult lqr = this.awsSQSClient.listQueues(new ListQueuesRequest(queueName));
		if(lqr.getQueueUrls() == null || lqr.getQueueUrls().size() != 1) throw new IllegalArgumentException("Failed to find one and only one queue named: "+queueName);
		return lqr.getQueueUrls().get(0);
	}
}
