package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.amazonaws.services.sqs.model.Message;

public class SingletonBeanMessageWorkerFactory implements MessageWorkerFactory {

	private SingletonWorker worker;

	@Required
	public void setWorker(SingletonWorker worker) {
		this.worker = worker;
	}

	@Override
	public Callable<List<Message>> createWorker(final List<Message> messages, final WorkerProgress workerProgress) {
		return new Callable<List<Message>>() {
			@Override
			public List<Message> call() throws Exception {
				return worker.call(messages, workerProgress);
			}
		};
	}
}
