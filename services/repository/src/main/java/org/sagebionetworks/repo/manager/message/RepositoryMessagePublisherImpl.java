package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;

/**
 * The basic implementation of the RepositoryMessagePublisher.  This implementation will publish all messages to an AWS topic
 * where external subscribers can receive notification of changes to the repository.
 * 
 * @author John
 *
 */
public class RepositoryMessagePublisherImpl implements RepositoryMessagePublisher {
	
	public static final String TOPIC_NAME_TEMPLATE = "%1$s-%2$s-repo-changes";
	
	@Autowired
	TransactionalMessenger transactionalMessanger;
	
	@Autowired
	AmazonSNSClient awsSNSClient;
	
	private String topicArn;
	
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
		CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(getTopicName()));
		topicArn = result.getTopicArn();
	}


	/**
	 * This is the method that the TransactionalMessenger will call after a transaction is committed.
	 * This is our chance to push these messages to our AWS topic.
	 */
	@Override
	public void fireChangeMessage(ChangeMessage message) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public String getTopicName(){
		return String.format(TOPIC_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}


	@Override
	public String getTopicArn() {
		return topicArn;
	}

}
