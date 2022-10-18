package org.sagebionetworks.worker.config;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.concurrent.ConcurrentManager;
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
import org.sagebionetworks.file.worker.FileHandleRestoreRequestWorker;
import org.sagebionetworks.migration.worker.MigrationWorker;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.report.worker.StorageReportCSVDownloadWorker;
import org.sagebionetworks.schema.worker.CreateJsonSchemaWorker;
import org.sagebionetworks.schema.worker.GetValidationSchemaWorker;
import org.sagebionetworks.table.worker.TableCSVAppenderPreviewWorker;
import org.sagebionetworks.table.worker.TableCSVDownloadWorker;
import org.sagebionetworks.table.worker.TableQueryNextPageWorker;
import org.sagebionetworks.table.worker.TableQueryWorker;
import org.sagebionetworks.table.worker.TableUpdateRequestWorker;
import org.sagebionetworks.table.worker.ViewColumnModelRequestWorker;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.worker.AsyncJobRunnerAdapter;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStack;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Configuration for workers that run asynchronous jobs ({@link AsyncJobRunner})
 */
@Configuration
public class AsyncJobWorkersConfig {
	
	private AmazonSQSClient amazonSQSClient;
	private StackConfiguration stackConfig;
	private AsynchJobStatusManager jobStatusManager;
	private UserManager userManager;
	private CountingSemaphore countingSemaphore;
	private StackStatusGate stackStatusGate;

	public AsyncJobWorkersConfig(AmazonSQSClient amazonSQSClient, StackConfiguration stackConfig, AsynchJobStatusManager jobStatusManager,
			UserManager userManager, CountingSemaphore countingSemaphore, StackStatusGate stackStatusGate) {
		this.amazonSQSClient = amazonSQSClient;
		this.stackConfig = stackConfig;
		this.jobStatusManager = jobStatusManager;
		this.userManager = userManager;
		this.countingSemaphore = countingSemaphore;
		this.stackStatusGate = stackStatusGate;
	}

	@Bean
	public SimpleTriggerFactoryBean tableQueryTrigger(ConcurrentManager concurrentStackManager, TableQueryWorker tableQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY");		
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableQueryWorker);
		
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
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableQueryNextPageWorker);
		
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
	
	@Bean
	public SimpleTriggerFactoryBean addFilesToDownloadListTrigger(AddFilesToDownloadListWorker addFilesToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_FILES_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addFilesToDownloadListWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("addFilesToDownloadList");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(3087)
			.withStartDelay(215)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean doiWorkerTrigger(DoiWorker doiWorker) {
		
		String queueName = stackConfig.getQueueName("DOI");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, doiWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("doiWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1367)
			.withStartDelay(217)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListAddWorkerTrigger(AddToDownloadListWorker addToDownloadListWorker) {
		
		String queueName = stackConfig.getQueueName("ADD_TO_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, addToDownloadListWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListAdd");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListQueryWorkerTrigger(DownloadListQueryWorker downloadListQueryWorker) {
		
		String queueName = stackConfig.getQueueName("QUERY_DOWNLOAD_LIST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListQueryWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListQuery");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListPackageWorkerTrigger(DownloadListPackageWorker downloadListPackageWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_PACKAGE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListPackageWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListPackage");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean downloadListManifestWorkerTrigger(DownloadListManifestWorker downloadListManifestWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_LIST_MANIFEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, downloadListManifestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("downloadListManifest");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1001)
			.withStartDelay(213)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleRestoreRequestWorkerTrigger(FileHandleRestoreRequestWorker fileHandleRestoreRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_RESTORE_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleRestoreRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(2);
		config.setSemaphoreLockKey("fileHandleRestoreRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2011)
			.withStartDelay(857)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileHandleArchivalRequestWorkerTrigger(FileHandleArchivalRequestWorker fileHandleArchivalRequestWorker) {
		
		String queueName = stackConfig.getQueueName("FILE_HANDLE_ARCHIVAL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, fileHandleArchivalRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(120);
		config.setSemaphoreMaxLockCount(1);
		config.setSemaphoreLockKey("fileHandleArchivalRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2014)
			.withStartDelay(517)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean fileBulkDownloadWorkerTrigger(BulkFileDownloadWorker bulkFileDownloadWorker) {
		
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
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2027)
			.withStartDelay(154)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean getValidationSchemaWorkerTrigger(GetValidationSchemaWorker getValidationSchemaWorker) {
		
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
		
		return new WorkerTriggerBuilder()
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
		
		// Migration can run in read-only mode
		config.setGate(null);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(600);
		config.setSemaphoreMaxLockCount(10);
		config.setSemaphoreLockKey("migration");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1000)
			.withStartDelay(154)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean schemaCreateWorkerTrigger(CreateJsonSchemaWorker createJsonSchemaWorker) {
		
		String queueName = stackConfig.getQueueName("JSON_SCHEMA_CREATE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, createJsonSchemaWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("jsonSchemaCreate");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1377)
			.withStartDelay(133)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean storageReportWorkerTrigger(StorageReportCSVDownloadWorker storageReportCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("STORAGE_REPORT");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, storageReportCSVDownloadWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(30);
		config.setSemaphoreMaxLockCount(2);
		config.setSemaphoreLockKey("storageReportWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1864)
			.withStartDelay(628)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVAppenderPreviewWorkerTrigger(TableCSVAppenderPreviewWorker tableCSVAppenderPreviewWorker) {
		
		String queueName = stackConfig.getQueueName("UPLOAD_CSV_TO_TABLE_PREVIEW");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVAppenderPreviewWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(4);
		config.setSemaphoreLockKey("tableCSVAppenderPreview");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1831)
			.withStartDelay(15)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableCSVDownloaderWorkerTrigger(TableCSVDownloadWorker tableCSVDownloadWorker) {
		
		String queueName = stackConfig.getQueueName("DOWNLOAD_CSV_FROM_TABLE");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableCSVDownloadWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(60);
		config.setSemaphoreMaxLockCount(4);
		config.setSemaphoreLockKey("tableCSVDownloader");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(2087)
			.withStartDelay(15)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean tableUpdateRequestWorkerTrigger(TableUpdateRequestWorker tableUpdateRequestWorker) {
		
		String queueName = stackConfig.getQueueName("TABLE_UPDATE_TRANSACTION");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, tableUpdateRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("tableUpdateRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1377)
			.withStartDelay(13)
			.build();
	}
	
	@Bean
	public SimpleTriggerFactoryBean viewColumnModelRequestWorkerTrigger(ViewColumnModelRequestWorker viewColumnModelRequestWorker) {
		
		String queueName = stackConfig.getQueueName("VIEW_COLUMN_MODEL_REQUEST");
		MessageDrivenRunner worker = new AsyncJobRunnerAdapter<>(jobStatusManager, userManager, viewColumnModelRequestWorker);
		
		MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
		
		config.setGate(stackStatusGate);
		config.setQueueName(queueName);
		config.setRunner(worker);
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(300);
		config.setSemaphoreMaxLockCount(8);
		config.setSemaphoreLockKey("viewColumnModelRequestWorker");
		
		MessageDrivenWorkerStack stack = new MessageDrivenWorkerStack(countingSemaphore, amazonSQSClient, config);
		
		return new WorkerTriggerBuilder()
			.withStack(stack)
			.withRepeatInterval(1053)
			.withStartDelay(850)
			.build();
	}

}
