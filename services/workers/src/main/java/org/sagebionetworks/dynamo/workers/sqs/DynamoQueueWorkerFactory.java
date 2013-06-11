package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorkerFactory implements MessageWorkerFactory{

	@Autowired
	private NodeTreeUpdateManager nodeTreeUpdateManager;

	@Autowired
	private Consumer consumer;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new DynamoQueueWorker(messages, this.nodeTreeUpdateManager, this.consumer);
	}
}
