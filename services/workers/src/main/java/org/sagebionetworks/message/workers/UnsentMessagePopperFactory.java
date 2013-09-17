package org.sagebionetworks.message.workers;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sqs.model.Message;

public class UnsentMessagePopperFactory implements MessageWorkerFactory {

	@Autowired
	private AmazonSNSClient awsSNSClient;
	private String topicName;
	private String topicArn;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	/**
	 * This is called by Spring when this bean is created
	 */
	public void initialize() {
		// Make sure the topic exists, if not create it
		CreateTopicResult result = awsSNSClient.createTopic(new CreateTopicRequest(topicName));
		topicArn = result.getTopicArn();
	}
	
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new UnsentMessagePopper(awsSNSClient, changeDAO, topicArn, messages);
	}

}
