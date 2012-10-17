package org.sagebionetworks.repo.manager.message;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
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

	static private Log log = LogFactory.getLog(RepositoryMessagePublisherImpl.class);

	@Autowired
	TransactionalMessenger transactionalMessanger;

	@Autowired
	AmazonSNSClient awsSNSClient;

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
	public void timerFired(){
		// Swap the current queue as an atomic action. Any messages that arrive while processing will get
		// processed the next time the timer fires.
		List<ChangeMessage> currentQueue = messageQueue.getAndSet(new LinkedList<ChangeMessage>());
		// Publish each message to the topic
		for(ChangeMessage message: currentQueue){
			try {
				String json = EntityFactory.createJSONStringForEntity(message);
				if(log.isTraceEnabled()){
					log.info("Publishing a message: "+json);
				}
				awsSNSClient.publish(new PublishRequest(this.topicArn, json));
			} catch (JSONObjectAdapterException e) {
				// This should not occur.
				// If it does we want to log it but continue to send messages
				// as this is called from a timer and not a web-services.
				log.error("Failed to parse ChangeMessage:", e);
			}
		}
	}
}
