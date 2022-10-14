package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManagerImpl;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentWorkerStack;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.doi.worker.DoiWorker;
import org.sagebionetworks.download.worker.AddToDownloadListWorker;
import org.sagebionetworks.download.worker.DownloadListManifestWorker;
import org.sagebionetworks.download.worker.DownloadListPackageWorker;
import org.sagebionetworks.download.worker.DownloadListQueryWorker;
import org.sagebionetworks.file.worker.AddFilesToDownloadListWorker;
import org.sagebionetworks.file.worker.BulkFileDownloadWorker;
import org.sagebionetworks.file.worker.FileHandleArchivalRequestWorker;
import org.sagebionetworks.file.worker.FileHandleAssociationScanDispatcherWorker;
import org.sagebionetworks.file.worker.FileHandleAssociationScanRangeWorker;
import org.sagebionetworks.file.worker.FileHandleKeysArchiveWorker;
import org.sagebionetworks.file.worker.FileHandleRestoreRequestWorker;
import org.sagebionetworks.file.worker.FileHandleStreamWorker;
import org.sagebionetworks.migration.worker.MigrationWorker;
import org.sagebionetworks.replication.workers.ObjectReplicationReconciliationWorker;
import org.sagebionetworks.replication.workers.ObjectReplicationWorker;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.report.worker.StorageReportCSVDownloadWorker;
import org.sagebionetworks.schema.worker.CreateJsonSchemaWorker;
import org.sagebionetworks.schema.worker.GetValidationSchemaWorker;
import org.sagebionetworks.ses.workers.SESNotificationWorker;
import org.sagebionetworks.table.worker.MaterializedViewSourceUpdateWorker;
import org.sagebionetworks.table.worker.MaterializedViewUpdateWorker;
import org.sagebionetworks.table.worker.TableCSVAppenderPreviewWorker;
import org.sagebionetworks.table.worker.TableCSVDownloadWorker;
import org.sagebionetworks.table.worker.TableIndexWorker;
import org.sagebionetworks.table.worker.TableQueryNextPageWorker;
import org.sagebionetworks.table.worker.TableQueryWorker;
import org.sagebionetworks.table.worker.TableUpdateRequestWorker;
import org.sagebionetworks.table.worker.TableViewWorker;
import org.sagebionetworks.table.worker.ViewColumnModelRequestWorker;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobRunnerAdapter;
import org.sagebionetworks.worker.TypedMessageDrivenRunnerAdapter;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStack;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class WorkersConfig {
	
	// Shared components
	private AmazonSQSClient amazonSQSClient;
	private StackConfiguration stackConfig;
	private AsynchJobStatusManager jobStatusManager;
	private UserManager userManager;
	private CountingSemaphore countingSemaphore;
	private ObjectMapper objectMapper;
	
	public WorkersConfig(AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, AsynchJobStatusManager jobStatusManager, UserManager userManager, CountingSemaphore countingSemaphore, ObjectMapper objectMapper) {
		this.amazonSQSClient = amazonSQSClient;
		this.stackConfig = stackConfig;
		this.jobStatusManager = jobStatusManager;
		this.userManager = userManager;
		this.countingSemaphore = countingSemaphore;
		this.objectMapper = objectMapper;
	}
	
	@Bean
	public StackStatusGate stackStatusGate() {
		return new StackStatusGate();
	}

	@Bean
	public ConcurrentManager concurrentStackManager(StackStatusDao stackStatusDao) {
		return new ConcurrentManagerImpl(countingSemaphore, amazonSQSClient, stackStatusDao);
	}
	
	@Bean
	public SimpleTriggerFactoryBean objectReplicationWorkerTrigger(ConcurrentManager concurrentStackManager, ObjectReplicationWorker objectReplicationWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_ENTITY_REPLICATION");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, objectReplicationWorker);
		
		return workerTriggerBuilder()
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
		
		return workerTriggerBuilder()
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
		
		return workerTriggerBuilder()
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
		
		return workerTriggerBuilder()
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
	public SimpleTriggerFactoryBean materializedViewWorkerTrigger(ConcurrentManager concurrentStackManager, MaterializedViewUpdateWorker materializedViewUpdateWorker) {
		
		String queueName = stackConfig.getQueueName("MATERIALIZED_VIEW_UPDATE");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, materializedViewUpdateWorker);
		
		return workerTriggerBuilder()
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
	public SimpleTriggerFactoryBean materializedViewSourceUpdateWorkerTrigger(ConcurrentManager concurrentStackManager, MaterializedViewSourceUpdateWorker materializedViewSourceUpdateWorker) {
		
		String queueName = stackConfig.getQueueName("MATERIALIZED_VIEW_SOURCE_UPDATE");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, materializedViewSourceUpdateWorker);
		
		return workerTriggerBuilder()
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
	public SimpleTriggerFactoryBean tableQueryTrigger(ConcurrentManager concurrentStackManager, TableQueryWorker tableQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY");		
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableQueryWorker);
		
		return workerTriggerBuilder()
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
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableQueryNextPageWorker);
		
		return workerTriggerBuilder()
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
	
	@Bean
	public SimpleTriggerFactoryBean addFilesToDownloadListTrigger(StackStatusGate stackStatusGate, AddFilesToDownloadListWorker addFilesToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_FILES_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addFilesToDownloadListWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("addFilesToDownloadList");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(3087)
			.withStartDelay(215)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean doiWorkerTrigger(StackStatusGate stackStatusGate, DoiWorker doiWorker) {
		
		String queueName = stackConfig.getQueueName("DOI");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, doiWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("doiWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1367)
			.withStartDelay(217)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListAddWorkerTrigger(StackStatusGate stackStatusGate, AddToDownloadListWorker addToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addToDownloadListWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListAdd");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListQueryWorkerTrigger(StackStatusGate stackStatusGate, DownloadListQueryWorker downloadListQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListQueryWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListQuery");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListPackageWorkerTrigger(StackStatusGate stackStatusGate, DownloadListPackageWorker downloadListPackageWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_PACKAGE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListPackageWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListPackage");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListManifestWorkerTrigger(StackStatusGate stackStatusGate, DownloadListManifestWorker downloadListManifestWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_MANIFEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListManifestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListManifest");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleRestoreRequestWorkerTrigger(StackStatusGate stackStatusGate, FileHandleRestoreRequestWorker fileHandleRestoreRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_RESTORE_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleRestoreRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(2);
		config.setSemaphoreLockKey("fileHandleRestoreRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2011)
			.withStartDelay(857)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleArchivalRequestWorkerTrigger(StackStatusGate stackStatusGate, FileHandleArchivalRequestWorker fileHandleArchivalRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_ARCHIVAL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleArchivalRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(1);
		config.setSemaphoreLockKey("fileHandleArchivalRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2014)
			.withStartDelay(517)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleKeysArchiveWorkerTrigger(StackStatusGate stackStatusGate, FileHandleKeysArchiveWorker fileHandleKeysArchiveWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_KEY_ARCHIVE");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, fileHandleKeysArchiveWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(10);
		config.setSemaphoreLockKey("fileHandleKeysArchiveWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(786)
			.withStartDelay(453)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleAssociationScanDispatcherWorkerTrigger(StackStatusGate stackStatusGate, FileHandleAssociationScanDispatcherWorker fileHandleAssociationScanDispatcherWorker) {
		
		SemaphoreGatedWorkerStackConfiguration workerConfig = new SemaphoreGatedWorkerStackConfiguration();
		
		workerConfig.setSemaphoreLockKey("fileHandleAssociationScanDispatcher");
		workerConfig.setProgressingRunner(fileHandleAssociationScanDispatcherWorker);
		workerConfig.setSemaphoreMaxLockCount(1);
		workerConfig.setSemaphoreLockTimeoutSec(60);
		workerConfig.setGate(stackStatusGate);
		
		return workerTriggerBuilder()
			.withStack(new SemaphoreGatedWorkerStack(countingSemaphore, workerConfig))
			// We do not need to check this often, we run this every 5 days. 
			.withRepeatInterval(1800000)
			// Note: the start delay is actually 2 hours, reason being that when the staging stack is first deployed it might take a while 
			// before we start migration which is when the stack is put to read-only mode.
			// If we do not wait for this the scanner will scan a mostly empty database delaying the next scan for at least 5 days.
			.withStartDelay(7200000)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleAssociationScanRangeWorkerTrigger(StackStatusGate stackStatusGate, FileHandleAssociationScanRangeWorker fileHandleAssociationScanRangeWorker) {
		
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
				
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1003)
			.withStartDelay(3465)
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
				
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1023)
			.withStartDelay(257)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileBulkDownloadWorkerTrigger(StackStatusGate stackStatusGate, BulkFileDownloadWorker bulkFileDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("BULK_FILE_DOWNLOAD");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, bulkFileDownloadWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(600);
		config.setSemaphoreMaxLockCount(4);
		config.setSemaphoreLockKey("fileBulkDownload");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2027)
			.withStartDelay(154)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean getValidationSchemaWorkerTrigger(StackStatusGate stackStatusGate, GetValidationSchemaWorker getValidationSchemaWorker) {
		
		String queueName = stackConfig.getQueueName("GET_VALIDATION_SCHEMA");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, getValidationSchemaWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("getValidationSchema");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1357)
			.withStartDelay(157)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean migrationWorkerTrigger(MigrationWorker migrationWorker) {
		
		String queueName = stackConfig.getQueueName("MIGRATION");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, migrationWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(600);
		config.setSemaphoreMaxLockCount(10);
		config.setSemaphoreLockKey("migration");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1000)
			.withStartDelay(154)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean schemaCreateWorkerTrigger(StackStatusGate stackStatusGate, CreateJsonSchemaWorker createJsonSchemaWorker) {
		
		String queueName = stackConfig.getQueueName("JSON_SCHEMA_CREATE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, createJsonSchemaWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("jsonSchemaCreate");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1377)
			.withStartDelay(133)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean storageReportWorkerTrigger(StackStatusGate stackStatusGate, StorageReportCSVDownloadWorker storageReportCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("STORAGE_REPORT");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, storageReportCSVDownloadWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(2);
		config.setSemaphoreLockKey("storageReportWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1864)
			.withStartDelay(628)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVAppenderPreviewWorkerTrigger(StackStatusGate stackStatusGate, TableCSVAppenderPreviewWorker tableCSVAppenderPreviewWorker) {
		
		String queueName = stackConfig.getQueueName("UPLOAD_CSV_TO_TABLE_PREVIEW");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVAppenderPreviewWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(4);
		config.setSemaphoreLockKey("tableCSVAppenderPreview");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1831)
			.withStartDelay(15)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVDownloaderWorkerTrigger(StackStatusGate stackStatusGate, TableCSVDownloadWorker tableCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_CSV_FROM_TABLE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVDownloadWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(4);
		config.setSemaphoreLockKey("tableCSVDownloader");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2087)
			.withStartDelay(15)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableUpdateRequestWorkerTrigger(StackStatusGate stackStatusGate, TableUpdateRequestWorker tableUpdateRequestWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_UPDATE_TRANSACTION");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableUpdateRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("tableUpdateRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1377)
			.withStartDelay(13)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean viewColumnModelRequestWorkerTrigger(StackStatusGate stackStatusGate, ViewColumnModelRequestWorker viewColumnModelRequestWorker) {
		
		String queueName = stackConfig.getQueueName("VIEW_COLUMN_MODEL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, viewColumnModelRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("viewColumnModelRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1053)
			.withStartDelay(850)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean sesNotificationWorkerTrigger(StackStatusGate stackStatusGate, SESNotificationWorker sesNotificationWorker) {
		
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
		
		return workerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1000)
			.withStartDelay(1971)
			.build();
	}
	
	static WorkerTriggerBuilder workerTriggerBuilder() {
		return new WorkerTriggerBuilder();
	}
	
	static class WorkerTriggerBuilder {

		private long startDelay;
		private long repeatInterval;
		private Object targetObject;
		
		private WorkerTriggerBuilder() {}
			
		public WorkerTriggerBuilder withStartDelay(long startDelay) {
			this.startDelay = startDelay;
			return this;
		}
		
		public WorkerTriggerBuilder withRepeatInterval(long repeatInterval) {
			this.repeatInterval = repeatInterval;
			return this;
		}
		
		public WorkerTriggerBuilder withStack(ConcurrentWorkerStack concurrentWorkerStack) {
			this.targetObject = concurrentWorkerStack;
			return this;
		}
		
		public WorkerTriggerBuilder withStack(SemaphoreGatedWorkerStack semaphoreGatedWorkerStack) {
			this.targetObject = semaphoreGatedWorkerStack;
			return this;
		}
		
		public WorkerTriggerBuilder withStack(MessageDrivenWorkerStack messageDrivenWorkerStack) {
			this.targetObject = messageDrivenWorkerStack;
			return this;
		}
			
		public SimpleTriggerFactoryBean build() {
			ValidateArgument.required(targetObject, "A stack");
			ValidateArgument.required(startDelay, "The startDelay");
			ValidateArgument.required(repeatInterval, "The repeatInterval");
			
			MethodInvokingJobDetailFactoryBean jobDetailFactory = new MethodInvokingJobDetailFactoryBean();		
			jobDetailFactory.setConcurrent(false);
			jobDetailFactory.setTargetMethod("run");
			jobDetailFactory.setTargetObject(targetObject);
			
			try {
				// Invoke the afterPropertiesSet here since this is not an exposed bean
				jobDetailFactory.afterPropertiesSet();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			
			SimpleTriggerFactoryBean triggerFactory = new SimpleTriggerFactoryBean();
			triggerFactory.setRepeatInterval(repeatInterval);
			triggerFactory.setStartDelay(startDelay);
			triggerFactory.setJobDetail(jobDetailFactory.getObject());
			
			return triggerFactory;
		}

	}

}
