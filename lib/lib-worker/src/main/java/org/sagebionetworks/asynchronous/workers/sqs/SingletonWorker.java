package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;

import com.amazonaws.services.sqs.model.Message;

abstract class SingletonWorker {

	private static final Logger log = LogManager.getLogger(SingletonWorker.class);

	/**
	 * Override this is you need to handle multiple messages at one time
	 * 
	 * @param messages
	 * @param workerProgress
	 * @return
	 * @throws Exception
	 */
	public List<Message> call(List<Message> messages, WorkerProgress workerProgress) throws Exception {
		// We should only get one message
		List<Message> toDelete = new LinkedList<Message>();
		for (Message message : messages) {
			try {
				Message returned = processMessage(message, workerProgress);
				if (returned != null) {
					toDelete.add(returned);
				}
			} catch (Throwable e) {
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	/**
	 * This is where the real work happens per message
	 * 
	 * @param message
	 * @return
	 * @throws Throwable
	 */
	protected abstract Message processMessage(Message message, WorkerProgress workerProgress) throws Throwable;
}
