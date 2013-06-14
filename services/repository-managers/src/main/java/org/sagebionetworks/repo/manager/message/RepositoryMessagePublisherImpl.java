package org.sagebionetworks.repo.manager.message;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * The basic implementation of the RepositoryMessagePublisher.  This implementation will publish all messages to an AWS topic
 * where external subscribers can receive notification of changes to the repository.
 * 
 * @author John
 *
 */
public class RepositoryMessagePublisherImpl implements RepositoryMessagePublisher {

	public static final String SEMAPHORE_KEY = "UNSENT_MESSAGE_WORKER";
	static private Log log = LogFactory.getLog(RepositoryMessagePublisherImpl.class);

	@Autowired
	TransactionalMessenger transactionalMessanger;

	@Autowired
	AmazonSNSClient awsSNSClient;
	
	@Autowired
	SemaphoreDao semaphoreDao;
	
	private boolean shouldMessagesBePublishedToTopic;
	
	private int listUnsentMessagePageSize;
	private long lockTimeoutMS;
	
	/**
	 * This is injected from spring.
	 * 
	 * @param shouldMessagesBePublishedToTopic
	 */
	public void setShouldMessagesBePublishedToTopic(
			boolean shouldMessagesBePublishedToTopic) {
		this.shouldMessagesBePublishedToTopic = shouldMessagesBePublishedToTopic;
	}

	/**
	 * This is injected from spring.
	 * @param listUnsentMessagePageSize
	 */
	public void setListUnsentMessagePageSize(int listUnsentMessagePageSize) {
		this.listUnsentMessagePageSize = listUnsentMessagePageSize;
	}

	/**
	 * Injected by spring
	 * @param lockTimeoutMS
	 */
	public void setLockTimeoutMS(long lockTimeoutMS) {
		this.lockTimeoutMS = lockTimeoutMS;
	}

	/**
	 * Default.
	 */
	public RepositoryMessagePublisherImpl(){
		super();
	}

	/**
	 * IoC constructor.
	 * @param transactionalMessanger
	 * @param awsSNSClient
	 * @param topicArn
	 * @param topicName
	 * @param messageQueue
	 */
	public RepositoryMessagePublisherImpl(TransactionalMessenger transactionalMessanger,
			AmazonSNSClient awsSNSClient) {
		super();
		this.transactionalMessanger = transactionalMessanger;
		this.awsSNSClient = awsSNSClient;
	}

	/**
	 * Used by tests to inject a mock client.
	 * @param awsSNSClient
	 */
	public void setAwsSNSClient(AmazonSNSClient awsSNSClient) {
		this.awsSNSClient = awsSNSClient;
	}

	private String topicArn;
	private String topicName;

	private AtomicReference<List<ChangeMessage>> messageQueue = 
			new AtomicReference<List<ChangeMessage>>(Collections.synchronizedList(new LinkedList<ChangeMessage>()));

	public RepositoryMessagePublisherImpl(final String topicName) {
		if (topicName == null) {
			throw new NullPointerException();
		}
		this.topicName = topicName;
	}

	/**
	 *
	 * This is called by Spring when this bean is created.  This is where we register this class as
	 * an observer of the TransactionalMessenger
	 */
	public void initialize(){
		// We only want to be in the list once
		transactionalMessanger.removeObserver(this);
		transactionalMessanger.registerObserver(this);
		// Make sure the topic exists, if not create it.
		// Is this a mock client?
		if(awsSNSClient.toString().startsWith("Mock for AmazonSNSClient")){
			// We have a mock client
			topicArn = "mockARN";
		}else{
			// We have  a real client.
			CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(getTopicName()));
			topicArn = result.getTopicArn();
		}
	}

	/**
	 * This is the method that the TransactionalMessenger will call after a transaction is committed.
	 * This is our chance to push these messages to our AWS topic.
	 */
	@Override
	public void fireChangeMessage(ChangeMessage message) {
		if(message == null) throw new IllegalArgumentException("ChangeMessage cannot be null");
		if(message.getChangeNumber()  == null) throw new IllegalArgumentException("ChangeMessage.getChangeNumber() cannot be null");
		if(message.getObjectId()  == null) throw new IllegalArgumentException("ChangeMessage.getObjectId() cannot be null");
		if(message.getObjectType()  == null) throw new IllegalArgumentException("ChangeMessage.getObjectType() cannot be null");
		if(message.getTimestamp()  == null) throw new IllegalArgumentException("ChangeMessage.getTimestamp() cannot be null");
		// Add the message to a queue
		messageQueue.get().add(message);
	}

	@Override
	public String getTopicName(){
		return topicName;
	}

	@Override
	public String getTopicArn() {
		return topicArn;
	}

	/**
	 * Quartz will fire this method on a timer.  This is where we actually publish the data. 
	 */
	@Override
	public void timerFired(){
		// Swap the current queue as an atomic action. Any messages that arrive while processing will get
		// processed the next time the timer fires.
		List<ChangeMessage> currentQueue = messageQueue.getAndSet(new LinkedList<ChangeMessage>());
		if(!shouldMessagesBePublishedToTopic){
			// The messages should not be broadcast
			if(log.isDebugEnabled() && currentQueue.size() > 0){
				log.debug("RepositoryMessagePublisherImpl.shouldBroadcast = false.  So "+currentQueue.size()+" messages will be thrown away.");
			}
			return;
		}
		// Publish each message to the topic
		for(ChangeMessage message: currentQueue){
			publishMessage(message);
		}
	}

	/**
	 * Publish the message and recored it as sent.
	 * 
	 * @param message
	 */
	private void publishMessage(ChangeMessage message) {
		try {
			String json = EntityFactory.createJSONStringForEntity(message);
			if(log.isTraceEnabled()){
				log.info("Publishing a message: "+json);
			}
			awsSNSClient.publish(new PublishRequest(this.topicArn, json));
			// Register the message was sent
			this.transactionalMessanger.registerMessageSent(message.getChangeNumber());
		} catch (JSONObjectAdapterException e) {
			// This should not occur.
			// If it does we want to log it but continue to send messages
			// as this is called from a timer and not a web-services.
			log.error("Failed to parse ChangeMessage:", e);
		}catch (NotFoundException e){
			// This can occur when we try to send a message that has already been deleted.
			// It is not really an error condition but we log it.
			if(log.isDebugEnabled()){
				log.debug(e.getMessage());
			}
		}
	}
	
	/**
	 * Quartz will fire this method on a 10 second timer. This timer is used to find messages that have been created but not sent.
	 *  We use a semaphore to ensure only one worker per stack does this task at a time.
	 */
	@Override
	public void timerFiredFindUnsentMessages(){
		// Do nothing if the messages should not be published.
		if(!shouldMessagesBePublishedToTopic) return;
		// We use a semaphore to ensure only one worker per stack does this task at a time.
		String lockToken = semaphoreDao.attemptToAcquireLock(SEMAPHORE_KEY, lockTimeoutMS);
		if(lockToken != null){
			log.debug("Acquire the lock with token: "+lockToken);
			try{
				// Add all messages to the queue.
				List<ChangeMessage> unSentMessages = transactionalMessanger.listUnsentMessages(this.listUnsentMessagePageSize);
				for(ChangeMessage message: unSentMessages){
					publishMessage(message);
				}
			}finally{
				// Release the lock
				boolean released = semaphoreDao.releaseLock(SEMAPHORE_KEY, lockToken);
				if(!released){
					log.warn("Failed to release the lock with token: "+lockToken);
				}
			}
		}else{
			log.debug("Did not acquire the lock.");
		}
	}
}
