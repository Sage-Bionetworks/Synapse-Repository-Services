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
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
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
	public SimpleTriggerFactoryBean addFilesToDownloadListTrigger(ConcurrentManager concurrentStackManager, AddFilesToDownloadListWorker addFilesToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_FILES_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addFilesToDownloadListWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("addFilesToDownloadList")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(3087)
		.withStartDelay(215)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean doiWorkerTrigger(ConcurrentManager concurrentStackManager, DoiWorker doiWorker) {
		
		String queueName = stackConfig.getQueueName("DOI");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, doiWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("doiWorker")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1367)
		.withStartDelay(217)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListAddWorkerTrigger(ConcurrentManager concurrentStackManager, AddToDownloadListWorker addToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addToDownloadListWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("downloadListAdd")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1001)
		.withStartDelay(213)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListQueryWorkerTrigger(ConcurrentManager concurrentStackManager, DownloadListQueryWorker downloadListQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListQueryWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("downloadListQuery")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1001)
		.withStartDelay(213)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListPackageWorkerTrigger(ConcurrentManager concurrentStackManager, DownloadListPackageWorker downloadListPackageWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_PACKAGE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListPackageWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("downloadListPackage")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1001)
		.withStartDelay(213)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListManifestWorkerTrigger(ConcurrentManager concurrentStackManager, DownloadListManifestWorker downloadListManifestWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_MANIFEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListManifestWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("downloadListManifest")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1001)
		.withStartDelay(213)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleRestoreRequestWorkerTrigger(ConcurrentManager concurrentStackManager, FileHandleRestoreRequestWorker fileHandleRestoreRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_RESTORE_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleRestoreRequestWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileHandleRestoreRequestWorker")
			.withSemaphoreMaxLockCount(2)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(2011)
		.withStartDelay(857)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleArchivalRequestWorkerTrigger(ConcurrentManager concurrentStackManager, FileHandleArchivalRequestWorker fileHandleArchivalRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_ARCHIVAL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleArchivalRequestWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileHandleArchivalRequestWorker")
			.withSemaphoreMaxLockCount(1)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(2014)
		.withStartDelay(517)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleKeysArchiveWorkerTrigger(ConcurrentManager concurrentStackManager, FileHandleKeysArchiveWorker fileHandleKeysArchiveWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_KEY_ARCHIVE");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, fileHandleKeysArchiveWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileHandleKeysArchiveWorker")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(786)
		.withStartDelay(453)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleAssociationScanRangeWorkerTrigger(ConcurrentManager concurrentStackManager, FileHandleAssociationScanRangeWorker fileHandleAssociationScanRangeWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_SCAN_REQUEST");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, fileHandleAssociationScanRangeWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileHandleAssociationScanRangeWorker")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1003)
		.withStartDelay(3465)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleStreamWorkerTrigger(ConcurrentManager concurrentStackManager, FileHandleStreamWorker fileHandleStreamWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_STREAM");
		MessageDrivenRunner worker = new ChangeMessageBatchProcessor(amazonSQSClient, queueName, fileHandleStreamWorker);
				
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileHandleStreamWorker")
			.withSemaphoreMaxLockCount(5)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(120)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1023)
		.withStartDelay(257)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileBulkDownloadWorkerTrigger(ConcurrentManager concurrentStackManager, BulkFileDownloadWorker bulkFileDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("BULK_FILE_DOWNLOAD");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, bulkFileDownloadWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("fileBulkDownload")
			.withSemaphoreMaxLockCount(4)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(600)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(2027)
		.withStartDelay(154)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean getValidationSchemaWorkerTrigger(ConcurrentManager concurrentStackManager, GetValidationSchemaWorker getValidationSchemaWorker) {
		
		String queueName = stackConfig.getQueueName("GET_VALIDATION_SCHEMA");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, getValidationSchemaWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("getValidationSchema")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1357)
		.withStartDelay(157)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean migrationWorkerTrigger(ConcurrentManager concurrentStackManager, MigrationWorker migrationWorker) {
		
		String queueName = stackConfig.getQueueName("MIGRATION");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, migrationWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("migration")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(600)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(true)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1000)
		.withStartDelay(154)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean schemaCreateWorkerTrigger(ConcurrentManager concurrentStackManager, CreateJsonSchemaWorker createJsonSchemaWorker) {
		
		String queueName = stackConfig.getQueueName("JSON_SCHEMA_CREATE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, createJsonSchemaWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("migration")
			.withSemaphoreMaxLockCount(10)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1377)
		.withStartDelay(133)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean storageReportWorkerTrigger(ConcurrentManager concurrentStackManager, StorageReportCSVDownloadWorker storageReportCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("STORAGE_REPORT");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, storageReportCSVDownloadWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("storageReportWorker")
			.withSemaphoreMaxLockCount(2)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(30)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1864)
		.withStartDelay(628)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVAppenderPreviewWorkerTrigger(ConcurrentManager concurrentStackManager, TableCSVAppenderPreviewWorker tableCSVAppenderPreviewWorker) {
		
		String queueName = stackConfig.getQueueName("UPLOAD_CSV_TO_TABLE_PREVIEW");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVAppenderPreviewWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("tableCSVAppenderPreview")
			.withSemaphoreMaxLockCount(4)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1831)
		.withStartDelay(15)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVDownloaderWorkerTrigger(ConcurrentManager concurrentStackManager, TableCSVDownloadWorker tableCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_CSV_FROM_TABLE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVDownloadWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("tableCSVDownloader")
			.withSemaphoreMaxLockCount(4)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(2087)
		.withStartDelay(15)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableUpdateRequestWorkerTrigger(ConcurrentManager concurrentStackManager, TableUpdateRequestWorker tableUpdateRequestWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_UPDATE_TRANSACTION");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableUpdateRequestWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("tableUpdateRequestWorker")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1377)
		.withStartDelay(13)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean viewColumnModelRequestWorkerTrigger(ConcurrentManager concurrentStackManager, ViewColumnModelRequestWorker viewColumnModelRequestWorker) {
		
		String queueName = stackConfig.getQueueName("VIEW_COLUMN_MODEL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, viewColumnModelRequestWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("viewColumnModelRequestWorker")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(300)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
		.withRepeatInterval(1053)
		.withStartDelay(850)
		.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean sesNotificationWorkerTrigger(ConcurrentManager concurrentStackManager, SESNotificationWorker sesNotificationWorker) {
		
		String queueName = stackConfig.getQueueName("SES_NOTIFICATIONS");
		MessageDrivenRunner worker = new TypedMessageDrivenRunnerAdapter<>(objectMapper, sesNotificationWorker);
		
		return workerTriggerBuilder()
			.withStack(ConcurrentWorkerStack.builder()
			.withSemaphoreLockKey("sesNotificationWorker")
			.withSemaphoreMaxLockCount(8)
			.withSemaphoreLockAndMessageVisibilityTimeoutSec(60)
			.withMaxThreadsPerMachine(1)
			.withSingleton(concurrentStackManager)
			.withCanRunInReadOnly(false)
			.withQueueName(queueName)
			.withWorker(worker)
			.build()
		)
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
