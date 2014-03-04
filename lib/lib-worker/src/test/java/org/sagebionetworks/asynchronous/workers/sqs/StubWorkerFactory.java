package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

/**
 * A simple stub used to control what works are passed.
 * @author John
 *
 */
public class StubWorkerFactory implements MessageWorkerFactory{
	
	Stack<StubWorker> workerStack;

	public StubWorkerFactory(Stack<StubWorker> workerQueue) {
		super();
		this.workerStack = workerQueue;
	}

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return workerStack.pop().withMessage(messages);
	}
}