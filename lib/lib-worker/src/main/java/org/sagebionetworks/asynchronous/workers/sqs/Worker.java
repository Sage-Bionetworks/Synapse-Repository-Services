package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.services.sqs.model.Message;

public interface Worker extends Callable<List<Message>> {

	void setMessages(List<Message> messages);

	void setWorkerProgress(WorkerProgress workerProgress);
}
