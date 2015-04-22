package org.sagebionetworks.asynchronous.workers.sqs;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.amazonaws.services.sqs.model.Message;

public class BeanCreatingMessageWorkerFactory implements MessageWorkerFactory {

	@Autowired
	private AutowireCapableBeanFactory factory;

	private Class<Worker> workerClass;

	@Required
	public void setWorker(Class<Worker> clazz) {
		this.workerClass = clazz;
	}

	@Override
	public Worker createWorker(List<Message> messages, WorkerProgress workerProgress) {
		Worker worker = factory.createBean(workerClass);
		worker.setMessages(messages);
		worker.setWorkerProgress(workerProgress);
		return worker;
	}
}
