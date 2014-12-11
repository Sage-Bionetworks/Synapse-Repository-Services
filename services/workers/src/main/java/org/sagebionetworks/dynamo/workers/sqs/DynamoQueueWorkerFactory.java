package org.sagebionetworks.dynamo.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.dynamo.NodeTreeUpdateManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class DynamoQueueWorkerFactory implements MessageWorkerFactory{

	@Autowired
	private NodeTreeUpdateManager nodeTreeUpdateManager;

	@Autowired
	private Consumer consumer;
	
	@Autowired
	private WorkerLogger workerLogger;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages, WorkerProgress workerProgress) {
		return new DynamoQueueWorker(messages, this.nodeTreeUpdateManager, this.consumer, workerLogger);
	}
}
