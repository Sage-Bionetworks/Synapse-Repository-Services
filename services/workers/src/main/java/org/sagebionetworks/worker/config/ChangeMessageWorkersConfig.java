package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.file.worker.FileHandleStreamWorker;
import org.sagebionetworks.replication.workers.ObjectReplicationReconciliationWorker;
import org.sagebionetworks.replication.workers.ObjectReplicationWorker;
import org.sagebionetworks.table.worker.MaterializedViewUpdateWorker;
import org.sagebionetworks.table.worker.TableIndexWorker;
import org.sagebionetworks.table.worker.TableViewWorker;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Configuration for workers that are driven by change messages
 */
@Configuration
public class ChangeMessageWorkersConfig {
	
	// Shared components
	private ConcurrentManager concurrentStackManager;
	private AmazonSQSClient amazonSQSClient;
	private StackConfiguration stackConfig;
	private CountingSemaphore countingSemaphore;
	
	public ChangeMessageWorkersConfig(ConcurrentManager concurrentStackManager, AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, CountingSemaphore countingSemaphore) {
		this.concurrentStackManager = concurrentStackManager;
		this.amazonSQSClient = amazonSQSClient;
		this.stackConfig = stackConfig;
		this.countingSemaphore = countingSemaphore;
	}

	@Bean
	public SimpleTriggerFactoryBean objectReplicationWorkerTrigger(ObjectReplicationWorker objectReplicationWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_ENTITY_REPLICATION");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, objectReplicationWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("objectReplication")
				.withSemaphoreMaxLockCount(10)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
				.withMaxThreadsPerMachine(5)
				.withSingleton(concurrentStackManager)
				.withCanRunInReadOnly(true)
				.withQueueName(queueName)
				.withWorker(worker)
				.build()
			)
			.withRepeatInterval(553)
			.withStartDelay(15)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean objectReplicationReconciliationWorkerTrigger(ObjectReplicationReconciliationWorker objectReplicationReconciliationWorker) {
		
		String queueName = stackConfig.getQueueName("ENTITY_REPLICATION_RECONCILIATION");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, objectReplicationReconciliationWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("objectReplicationReconciliationWorker")
				.withSemaphoreMaxLockCount(10)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
				.withMaxThreadsPerMachine(5)
				.withSingleton(concurrentStackManager)
				.withCanRunInReadOnly(false)
				.withQueueName(queueName)
				.withWorker(worker)
				.build()
			)
			.withRepeatInterval(2034)
			.withStartDelay(17)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableIndexWorkerTrigger(TableIndexWorker tableIndexWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_UPDATE");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, tableIndexWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("tableIndexWorker")
				.withSemaphoreMaxLockCount(10)
				.withSemaphoreLockAndMessageVisibilityTimeoutSec(1200)
				.withMaxThreadsPerMachine(10)
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
	public SimpleTriggerFactoryBean tableViewWorkerTrigger(TableViewWorker tableViewWorker) {
		
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
	
	@Bean
	public SimpleTriggerFactoryBean materializedViewWorkerTrigger(MaterializedViewUpdateWorker materializedViewUpdateWorker) {
		
		String queueName = stackConfig.getQueueName("MATERIALIZED_VIEW_UPDATE");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, materializedViewUpdateWorker);
		
		return new WorkerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
				.withSemaphoreLockKey("materializedViewUpdate")
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
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleStreamWorkerTrigger(StackStatusGate stackStatusGate, FileHandleStreamWorker fileHandleStreamWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_STREAM");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, fileHandleStreamWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(5);
		config.setSemaphoreLockKey("fileHandleStreamWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
				
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1023)
			.withStartDelay(257)
			.build();
	}

}
