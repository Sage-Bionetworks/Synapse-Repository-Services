package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.file.worker.FileHandleAssociationScanRangeWorker;
import org.sagebionetworks.file.worker.FileHandleKeysArchiveWorker;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.ses.workers.SESNotificationWorker;
import org.sagebionetworks.table.worker.MaterializedViewSourceUpdateWorker;
import org.sagebionetworks.worker.TypedMessageDrivenRunnerAdapter;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for workers that are driven by generic SQS messages (For {@link ChangeMessage} driven workers see {@link ChangeMessageWorkersConfig})
 */
@Configuration
public class MessageDrivenWorkersConfig {
	
	private ConcurrentManager concurrentStackManager;
	private StackStatusGate stackStatusGate; 
	private AmazonSQSClient amazonSQSClient;
	private StackConfiguration stackConfig;
	private CountingSemaphore countingSemaphore;
	private ObjectMapper objectMapper;

	public MessageDrivenWorkersConfig(ConcurrentManager concurrentStackManager, StackStatusGate stackStatusGate, AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, CountingSemaphore countingSemaphore,
			ObjectMapper objectMapper) {
		this.concurrentStackManager = concurrentStackManager;
		this.stackStatusGate = stackStatusGate;
		this.amazonSQSClient = amazonSQSClient;
		this.stackConfig = stackConfig;
		this.countingSemaphore = countingSemaphore;
		this.objectMapper = objectMapper;
	}

	@Bean
	public SimpleTriggerFactoryBean materializedViewSourceUpdateWorkerTrigger(MaterializedViewSourceUpdateWorker materializedViewSourceUpdateWorker) {
		
		String queueName = stackConfig.getQueueName("MATERIALIZED_VIEW_SOURCE_UPDATE");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, materializedViewSourceUpdateWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("materializedViewSourceUpdateWorker")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
			.withMaxThreadsPerMachine(2)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(true)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(934)
		.withStartDelay(578)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleKeysArchiveWorkerTrigger(FileHandleKeysArchiveWorker fileHandleKeysArchiveWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_KEY_ARCHIVE");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, fileHandleKeysArchiveWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(10);
		config.setSemaphoreLockKey("fileHandleKeysArchiveWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(786)
			.withStartDelay(453)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleAssociationScanRangeWorkerTrigger(FileHandleAssociationScanRangeWorker fileHandleAssociationScanRangeWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_SCAN_REQUEST");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, fileHandleAssociationScanRangeWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(10);
		config.setSemaphoreLockKey("fileHandleAssociationScanRangeWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
				
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1003)
			.withStartDelay(3465)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean sesNotificationWorkerTrigger(SESNotificationWorker sesNotificationWorker) {
		
		String queueName = stackConfig.getQueueName("SES_NOTIFICATIONS");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, sesNotificationWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("sesNotificationWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1000)
			.withStartDelay(1971)
			.build();
	}

}
