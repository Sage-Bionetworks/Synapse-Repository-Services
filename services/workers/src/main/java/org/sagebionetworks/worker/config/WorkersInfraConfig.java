package org.sagebionetworks.worker.config;

import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManagerImpl;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
public class WorkersInfraConfig {
		
	@Bean
	public StackStatusGate stackStatusGate() {
		return new StackStatusGate();
	}

	@Bean
	public ConcurrentManager concurrentStackManager(CountingSemaphore countingSemaphore, AmazonSQSClient amazonSQSClient, StackStatusDao stackStatusDao) {
		return new ConcurrentManagerImpl(countingSemaphore, amazonSQSClient, stackStatusDao);
	}

}
