package org.sagebionetworks.repo.manager.message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	
	private boolean shouldMessagesBePublishedToTopic;
	
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

	private ConcurrentLinkedQueue<ChangeMessage> messageQueue = new ConcurrentLinkedQueue<ChangeMessage>();

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
		messageQueue.add(message);
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
		// Poll all data from the queue.
		List<ChangeMessage> currentQueue = pollListFromQueue();
		if(!shouldMessagesBePublishedToTopic){
			// The messages should not be broadcast
			if(log.isDebugEnabled() && currentQueue.size() > 0){
				log.debug("RepositoryMessagePublisherImpl.shouldBroadcast = false.  So "+currentQueue.size()+" messages will be thrown away.");
			}
			return;
		}
		// Publish each message to the topic
		for(ChangeMessage message: currentQueue){
			publishToTopic(message);
		}
	}
	
	/**
	 * Poll all data currently on the queue and add it to a list.
	 * @return
	 */
	private List<ChangeMessage> pollListFromQueue(){
		List<ChangeMessage> list = new LinkedList<ChangeMessage>();
		for(ChangeMessage cm = this.messageQueue.poll(); cm != null; cm = this.messageQueue.poll()){
			// Add to the list
			list.add(cm);
		}
		return list;
	}

	/**
	 * Publish the message and recored it as sent.
	 * 
	 * @param message
	 */
	@Override
	public void publishToTopic(ChangeMessage message) {
		try {
			String json = EntityFactory.createJSONStringForEntity(message);
			if(log.isTraceEnabled()){
				log.info("Publishing a message: "+json);
			}
			awsSNSClient.publish(new PublishRequest(this.topicArn, json));
			// Register the message was sent
			this.transactionalMessanger.registerMessageSent(message);
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
}
