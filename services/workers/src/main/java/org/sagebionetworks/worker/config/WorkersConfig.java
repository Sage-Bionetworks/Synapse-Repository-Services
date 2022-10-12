package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManagerImpl;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.replication.workers.ObjectReplicationReconciliationWorker;
import org.sagebionetworks.replication.workers.ObjectReplicationWorker;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.table.worker.TableIndexWorker;
import org.sagebionetworks.table.worker.TableQueryNextPageWorker;
import org.sagebionetworks.table.worker.TableQueryWorker;
import org.sagebionetworks.table.worker.TableViewWorker;
import org.sagebionetworks.worker.AsyncJobRunnerAdapter;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;

@Configuration
public class WorkersConfig {
	
	// Shared components
	private AmazonSQSClient amazonSQSClient;
	private StackConfiguration stackConfig;
	private AsynchJobStatusManager jobStatusManager;
	private UserManager userManager;
	
	public WorkersConfig(AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, AsynchJobStatusManager jobStatusManager, UserManager userManager) {
		this.amazonSQSClient = amazonSQSClient;
		this.stackConfig = stackConfig;
		this.jobStatusManager = jobStatusManager;
		this.userManager = userManager;
	}

	@Bean
	public ConcurrentManager concurrentStackManager(CountingSemaphore countingSemaphore, StackStatusDao stackStatusDao) {
		return new ConcurrentManagerImpl(countingSemaphore, amazonSQSClient, stackStatusDao);
	}
	
	@Bean
	public SimpleTriggerFactoryBean objectReplicationWorkerTrigger(ConcurrentManager concurrentStackManager, ObjectReplicationWorker objectReplicationWorker) {
		
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
	public SimpleTriggerFactoryBean objectReplicationReconciliationWorkerTrigger(ConcurrentManager concurrentStackManager, ObjectReplicationReconciliationWorker objectReplicationReconciliationWorker) {
		
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
	public SimpleTriggerFactoryBean tableIndexWorkerTrigger(ConcurrentManager concurrentStackManager, TableIndexWorker tableIndexWorker) {
		
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
	public SimpleTriggerFactoryBean tableViewWorkerTrigger(ConcurrentManager concurrentStackManager, TableViewWorker tableViewWorker) {
		
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
	public SimpleTriggerFactoryBean tableQueryTrigger(ConcurrentManager concurrentStackManager, TableQueryWorker tableQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY");		
		AsyncJobRunnerAdapter<?, ?> worker = new AsyncJobRunnerAdapter<>(tableQueryWorker);
		worker.configure(jobStatusManager, userManager);
		
		return new WorkerTriggerBuilder()
				.withStack(ConcurrentWorkerStack.builder()
					.withSemaphoreLockKey("tableQueryWorker")
					.withSemaphoreMaxLockCount(10)
					.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
					.withMaxThreadsPerMachine(3)
					.withSingleton(concurrentStackManager)
					.withCanRunInReadOnly(false)
					.withQueueName(queueName)
					.withWorker(worker)
					.build()
				)
				.withRepeatInterval(2187)
				.withStartDelay(1025)
				.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableQueryNextPageTrigger(ConcurrentManager concurrentStackManager, TableQueryNextPageWorker tableQueryNextPageWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY_NEXT_PAGE");		
		AsyncJobRunnerAdapter<?, ?> worker = new AsyncJobRunnerAdapter<>(tableQueryNextPageWorker);
		worker.configure(jobStatusManager, userManager);
		
		return new WorkerTriggerBuilder()
				.withStack(ConcurrentWorkerStack.builder()
					.withSemaphoreLockKey("tableQueryNextPageWorker")
					.withSemaphoreMaxLockCount(10)
					.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
					.withMaxThreadsPerMachine(3)
					.withSingleton(concurrentStackManager)
					.withCanRunInReadOnly(false)
					.withQueueName(queueName)
					.withWorker(worker)
					.build()
				)
				.withRepeatInterval(2180)
				.withStartDelay(1024)
				.build();
	}

}
