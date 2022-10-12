package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManagerImpl;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.table.worker.TableIndexWorker;
import org.sagebionetworks.table.worker.TableViewWorker;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
public class WorkersConfig {
		
	@Bean
	public ConcurrentManager concurrentStackManager(CountingSemaphore countingSemaphore, AmazonSQSClient amazonSQSClient, StackStatusDao stackStatusDao) {
		return new ConcurrentManagerImpl(countingSemaphore, amazonSQSClient, stackStatusDao);
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableIndexWorkerTrigger(ConcurrentManager concurrentStackManager, AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, TableIndexWorker tableIndexWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_UPDATE");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, tableIndexWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("tableIndexWorker")
				.withSemaphoreMaxLockCount(10)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(1200)
				.withMaxThreadsPerMachine(3)
				.withSingleton(concurrentStackManager)
				.withCanRunInReadOnly(true)
				.withQueueName(queueName)
				.withWorker(worker)
				.build()
			)
			.withRepeatInterval(1797)
			.withStartDelay(256)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableViewWorkerTrigger(ConcurrentManager concurrentStackManager, AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, TableViewWorker tableViewWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_VIEW");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, tableViewWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("tableViewWorker")
				.withSemaphoreMaxLockCount(10)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
				.withMaxThreadsPerMachine(3)
				.withSingleton(concurrentStackManager)
				.withCanRunInReadOnly(true)
				.withQueueName(queueName)
				.withWorker(worker)
				.build()
			)
			.withRepeatInterval(750)
			.withStartDelay(253)
			.build();
	}

}
