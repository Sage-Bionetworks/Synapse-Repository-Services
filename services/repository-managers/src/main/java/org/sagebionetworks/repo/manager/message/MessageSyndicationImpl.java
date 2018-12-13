package org.sagebionetworks.repo.manager.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.PublishResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;

/**
 * Basic implementation of the message syndication.
 * 
 * @author jmhill
 *
 */
public class MessageSyndicationImpl implements MessageSyndication {
	
	static private Log log = LogFactory.getLog(MessageSyndicationImpl.class);
	
	public static final Long DEFAULT_LIMIT = new Long(10);
	
	@Autowired
	RepositoryMessagePublisher messagePublisher;
	
	@Autowired
	private AmazonSQS awsSQSClient;
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	public MessageSyndicationImpl() {
		super();
	}

	public MessageSyndicationImpl(RepositoryMessagePublisher messagePublisher, AmazonSQS awsSQSClient, DBOChangeDAO changeDAO) {
		super();
		this.messagePublisher = messagePublisher;
		this.awsSQSClient = awsSQSClient;
		this.changeDAO = changeDAO;
	}

	@Override
	public void rebroadcastAllChangeMessages() {
		this.rebroadcastChangeMessages(0L, Long.MAX_VALUE);
	}
	
	@Override
	public long rebroadcastChangeMessages(Long startChangeNumber, Long limit) {
		if (startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		long nextNumber = -1;
		// Get the batch
		List<ChangeMessage> list = changeDAO.listChanges(startChangeNumber, null, limit);;
		for(ChangeMessage change: list){
			messagePublisher.fireChangeMessage(change);
			nextNumber = change.getChangeNumber()+1;
		}		
		// Return -1 if all msgs done
		long currentChangeNumber = changeDAO.getCurrentChangeNumber();
		if(nextNumber > currentChangeNumber){
			nextNumber = -1;
		}
		return nextNumber;
	}
	
	@Override
	public long getCurrentChangeNumber() {
		long lastChangeNumber = changeDAO.getCurrentChangeNumber();
		return lastChangeNumber;
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
	public PublishResults rebroadcastChangeMessagesToQueue(String queueName, ObjectType type, Long startChangeNumber, Long limit) {
		if(queueName == null) throw new IllegalArgumentException("queueName cannot be null");
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		// If the limit is null then default to 10
		if(limit == null){
			limit = DEFAULT_LIMIT;
		}
		// Look up the URL fo the Queue
		String queUrl = lookupQueueURL(queueName);
		// List all change messages
		List<PublishResult> resultList = new LinkedList<PublishResult>();
		List<ChangeMessage> list = null;
		long remaining = limit;
		long lastNumber = startChangeNumber;
		do{
			long localLimit = Math.min(10, remaining);
			// Query for a sub-set of the messages.
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
				SendMessageBatchResult batchResults = awsSQSClient.sendMessageBatch(new SendMessageBatchRequest(queUrl, batch));
				resultList.addAll(prepareResults(list, batchResults));
			}
			remaining -= list.size();
		}while(list.size() > 0 && remaining > 0);
		// return the count
		PublishResults prs = new PublishResults();
		prs.setList(resultList);
		return prs;
	}

	/**
	 * Convert the AWS results into simple results.
	 * @param list
	 * @param results
	 * @return
	 */
	public List<PublishResult> prepareResults(List<ChangeMessage> list, SendMessageBatchResult results){
		// Convert the results
		List<PublishResult> prList = new LinkedList<PublishResult>();
		// record success
		if(results.getSuccessful() != null){
			for(SendMessageBatchResultEntry smbre: results.getSuccessful()){
				PublishResult pr = new PublishResult();
				int index = Integer.parseInt(smbre.getId());
				ChangeMessage cm = list.get(index);
				pr.setChangeNumber(cm.getChangeNumber());
				pr.setSuccess(true);
				prList.add(pr);
			}
		}
		// record failures
		if(results.getFailed() != null){
			for(BatchResultErrorEntry bree: results.getFailed()){
				PublishResult pr = new PublishResult();
				int index = Integer.parseInt(bree.getId());
				ChangeMessage cm = list.get(index);
				pr.setChangeNumber(cm.getChangeNumber());
				pr.setSuccess(false);
				prList.add(pr);
			}
		}
		return prList;
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
		GetQueueUrlResult res = null;
		try {
			res = this.awsSQSClient.getQueueUrl(queueName);
		} catch (QueueDoesNotExistException e) {
			throw new IllegalArgumentException("Failed to find a queue named: " + queueName);
		}
		return res.getQueueUrl();
	}

	/**
	 * List change messages.
	 */
	@Override
	public ChangeMessages listChanges(Long startChangeNumber, ObjectType type,	Long limit) {
		if(startChangeNumber == null) throw new IllegalArgumentException("startChangeNumber cannot be null");
		// If the limit is null then default to 10
		if(limit == null){
			limit = DEFAULT_LIMIT;
		}
		List<ChangeMessage> list = changeDAO.listChanges(startChangeNumber, type, limit);
		ChangeMessages results = new ChangeMessages();
		results.setList(list);
		return results;
	}
}
