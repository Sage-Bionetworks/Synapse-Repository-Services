package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.sqs.model.Message;

public abstract class AbstractWorker implements Worker {

	private static final Logger log = LogManager.getLogger(AbstractWorker.class);

	private WorkerProgress workerProgress;
	private List<Message> messages;

	@Override
	public List<Message> call() throws Exception {
		// We should only get one message
		List<Message> toDelete = new LinkedList<Message>();
		for (Message message : messages) {
			try {
				toDelete.add(processMessage(message));
			} catch (Throwable e) {
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	public WorkerProgress getWorkerProgress() {
		return workerProgress;
	}

	/**
	 * This is where the real work happens
	 * 
	 * @param message
	 * @return
	 * @throws Throwable
	 */
	protected abstract Message processMessage(Message message) throws Throwable;
}
