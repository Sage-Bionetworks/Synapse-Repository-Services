package org.sagebionetworks.repo.manager.message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.Message;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.common.collect.Lists;

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
	AmazonSNS awsSNSClient;
	
	private boolean shouldMessagesBePublishedToTopic;

	// The prefix applied to each topic.
	private final String topicPrefix;

	// The name for the modification topic.
	private final String modificationTopicName;
	
	// Maps each object type to its topic
	Map<ObjectType, TopicInfo> typeToTopicMap = new HashMap<ObjectType, TopicInfo>();;

	private TopicInfo modificationTopic;

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
	 * IoC constructor.
	 * @param transactionalMessanger
	 * @param awsSNSClient
	 * @param topicArn
	 * @param topicName
	 * @param messageQueue
	 */
	public RepositoryMessagePublisherImpl(String topicPrefix, String modificationTopicName, TransactionalMessenger transactionalMessanger,
			AmazonSNS awsSNSClient) {
		this.topicPrefix = topicPrefix;
		this.modificationTopicName = modificationTopicName;
		this.transactionalMessanger = transactionalMessanger;
		this.awsSNSClient = awsSNSClient;
	}

	/**
	 * Used by tests to inject a mock client.
	 * @param awsSNSClient
	 */
	public void setAwsSNSClient(AmazonSNS awsSNSClient) {
		this.awsSNSClient = awsSNSClient;
	}


	private ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();

	public RepositoryMessagePublisherImpl(final String topicPrefix, final String modificationTopicName) {
		ValidateArgument.required(modificationTopicName, "modificationTopicName");
		ValidateArgument.required(topicPrefix, "topicPrefix");
		this.topicPrefix = topicPrefix;
		this.modificationTopicName = modificationTopicName;
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
	public String getTopicName(ObjectType type){
		return getTopicInfoLazy(type).getName();
	}

	@Override
	public String getTopicArn(ObjectType type) {
		return getTopicInfoLazy(type).getArn();
	}

	/**
	 * Quartz will fire this method on a timer.  This is where we actually publish the data. 
	 */
	@Override
	public void timerFired(){
		// Poll all data from the queue.
		List<Message> currentQueue = pollListFromQueue();
		if(!shouldMessagesBePublishedToTopic){
			// The messages should not be broadcast
			if(log.isDebugEnabled() && currentQueue.size() > 0){
				log.debug("RepositoryMessagePublisherImpl.shouldBroadcast = false.  So "+currentQueue.size()+" messages will be thrown away.");
			}
			return;
		}
		// Publish each message to the topic
		for (Message queueItem : currentQueue) {
			try {
				if (queueItem instanceof ChangeMessage) {
					ChangeMessage message = (ChangeMessage) queueItem;
					publishToTopic(message);
				} else {
					throw new IllegalArgumentException("Unknown message type " + queueItem.getClass().getName());
				}
			} catch (Throwable e) {
				// If one messages fails, we must send the rest.
				log.error("Failed to publish message.", e);
			}
		}
	}
	
	/**
	 * Poll all data currently on the queue and add it to a list.
	 * @return
	 */
	private List<Message> pollListFromQueue() {
		List<Message> list = Lists.newLinkedList();
		for (Message cm = this.messageQueue.poll(); cm != null; cm = this.messageQueue.poll()) {
			// Add to the list
			list.add(cm);
		}
		return list;
	}

	/**
	 * Get the topic info for a given type (lazy loaded).
	 * 
	 * @param type
	 * @return
	 */
	private TopicInfo getTopicInfoLazy(ObjectType type){
		if(type == null){
			throw new IllegalArgumentException("ObjectType cannot be null");
		}
		TopicInfo info = this.typeToTopicMap.get(type);
		if(info == null){
			// Create the topic
			String name = this.topicPrefix+type.name();
			CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(name));
			String arn = result.getTopicArn();
			info = new TopicInfo(name, arn);
			this.typeToTopicMap.put(type, info);
		}
		return info;
	}

	/**
	 * Get the topic info for modifications (lazy loaded).
	 * 
	 * @param type
	 * @return
	 */
	private TopicInfo getModificationTopicInfoLazy() {
		if (this.modificationTopic == null) {
			CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(this.modificationTopicName));
			String arn = result.getTopicArn();
			this.modificationTopic = new TopicInfo(this.modificationTopicName, arn);
		}
		return this.modificationTopic;
	}

	/**
	 * Publish the message and recored it as sent. Each sent message requires its own transaction.
	 * 
	 * @param message
	 */
	@NewWriteTransaction
	@Override
	public void publishToTopic(ChangeMessage message) {
		// treat as a batch of size one.
		publishBatchToTopic(message.getObjectType(), Arrays.asList(message));
	}
	
	
	@NewWriteTransaction
	@Override
	public void publishBatchToTopic(ObjectType type, List<ChangeMessage> batch) {
		if (type == null) {
			throw new IllegalArgumentException("Type cannot be null");
		}
		if (batch == null) {
			throw new IllegalArgumentException("Batch cannot be null");
		}
		if (batch.size() > ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE) {
			throw new IllegalArgumentException(
					"Batch size is limited to the number of messages that can be written to a single SQS Messages body.  Current maximumn: "
							+ ChangeMessageUtils.MAX_NUMBER_OF_CHANGE_MESSAGES_PER_SQS_MESSAGE);
		}
		// Write the batch to sent table.
		this.transactionalMessanger.registerMessagesSent(type, batch);
		// Lookup the topic arn.
		String topicArn = getTopicInfoLazy(type).getArn();
		ChangeMessages messages = new ChangeMessages();
		messages.setList(batch);
		// publish the batch to to the topic
		publish(messages, topicArn);
	}

	private void publish(JSONEntity message, String topicArn) {
		String json;
		try {
			json = EntityFactory.createJSONStringForEntity(message);
		} catch (JSONObjectAdapterException e) {
			// should never occur
			throw new RuntimeException(e);
		}
		if (log.isTraceEnabled()) {
			log.trace("Publishing a message: " + json);
		}
		// Publish the message to the topic.
		awsSNSClient.publish(new PublishRequest(topicArn, json));
	}
	
	/**
	 * Information about a topic.
	 *
	 */
	private static class TopicInfo{
		private String name;
		private String arn;
		public TopicInfo(String name, String arn) {
			super();
			this.name = name;
			this.arn = arn;
		}
		public String getName() {
			return name;
		}
		public String getArn() {
			return arn;
		}
	}

}
