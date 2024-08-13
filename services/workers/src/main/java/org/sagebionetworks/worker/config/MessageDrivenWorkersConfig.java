package org.sagebionetworks.worker.config;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.file.worker.FileHandleAssociationScanRangeWorker;
import org.sagebionetworks.file.worker.FileHandleKeysArchiveWorker;
import org.sagebionetworks.file.worker.FileEventRecordWorker;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.ses.workers.SESNotificationWorker;
import org.sagebionetworks.table.worker.MaterializedViewSourceUpdateWorker;
import org.sagebionetworks.table.worker.TableSnapshotWorker;
import org.sagebionetworks.table.worker.UpdateQueryCacheWorker;
import org.sagebionetworks.webhook.workers.WebhookMessageWorker;
import org.sagebionetworks.worker.JsonEntityDrivenRunnerAdapter;
import org.sagebionetworks.worker.TypedMessageDrivenRunnerAdapter;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

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
		MessageDrivenRunner worker = new JsonEntityDrivenRunnerAdapter<>(materializedViewSourceUpdateWorker);
		
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
	
	@Bean
	public SimpleTriggerFactoryBean tableSnapshotWorkerTrigger(TableSnapshotWorker tableSnapshotWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_SNAPSHOTS");
		MessageDrivenRunner worker = new JsonEntityDrivenRunnerAdapter<>(tableSnapshotWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("tableSnapshotWorker")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
			.withMaxThreadsPerMachine(2)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(true)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(937)
		.withStartDelay(1045)
		.build();
	}

	@Bean
	public SimpleTriggerFactoryBean fileEventRecordWorkerTrigger(FileEventRecordWorker fileEventRecordWorker) {

		String queueName = stackConfig.getQueueName("FILE_EVENT_RECORDS");
		MessageDrivenRunner worker = new JsonEntityDrivenRunnerAdapter<>(fileEventRecordWorker);

		return new WorkerTriggerBuilder()
				.withStack(ConcurrentWorkerStack.builder()
						.withSemaphoreLockKey("fileEventRecordWorker")
						.withSemaphoreMaxLockCount(5)
						.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
						.withMaxThreadsPerMachine(1)
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
	public SimpleTriggerFactoryBean updateQueryCacheTrigger(UpdateQueryCacheWorker cacheWorker) {

		String queueName = stackConfig.getQueueName("UPDATE_QUERY_CACHE");
		MessageDrivenRunner worker = new JsonEntityDrivenRunnerAdapter<>(cacheWorker);

		return new WorkerTriggerBuilder()
				.withStack(ConcurrentWorkerStack.builder()
						.withSemaphoreLockKey("updateQueryCacheWorker")
						.withSemaphoreMaxLockCount(5)
						.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
						.withMaxThreadsPerMachine(10)
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
	public SimpleTriggerFactoryBean webhookMessageWorkerTrigger(WebhookMessageWorker webhookMessageWorker) {

		String queueName = stackConfig.getQueueName("WEBHOOK_MESSAGE");
		MessageDrivenRunner worker = new JsonEntityDrivenRunnerAdapter<>(webhookMessageWorker);

		return new WorkerTriggerBuilder()
				.withStack(ConcurrentWorkerStack.builder()
						.withSemaphoreLockKey("webhookMessageWorker")
						.withSemaphoreMaxLockCount(5)
						.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
						.withMaxThreadsPerMachine(10)
						.withSingleton(concurrentStackManager)
						.withCanRunInReadOnly(true)
						.withQueueName(queueName)
						.withWorker(worker)
						.build()
				)
				.withRepeatInterval(976)
				.withStartDelay(1045)
				.build();
	}
}
