package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.dynamo.manager.NodeTreeUpdateManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorkerFactory implements MessageWorkerFactory{

	@Autowired
	private NodeTreeUpdateManager nodeTreeUpdateManager;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new DynamoQueueWorker(messages, this.nodeTreeUpdateManager);
	}
}
