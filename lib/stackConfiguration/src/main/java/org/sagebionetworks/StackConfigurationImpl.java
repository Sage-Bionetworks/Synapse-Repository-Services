package org.sagebionetworks;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

import com.google.inject.Inject;

public class StackConfigurationImpl implements StackConfiguration {

	private final String PROD = "prod";
	private final String DEV = "dev";
	private final String HUDSON = "hud";

	// Package-scoped for unit tests.
	final static String CONFIG_KEY_STS_IAM_ARN = "org.sagebionetworks.sts.iam.arn";
	private static final String SERVICE_AUTH_TEMPLATE = "org.sagebionetworks.%s.auth.%s";

	private ConfigurationProperties configuration;
	private StackEncrypter stackEncrypter;
	
	/**
	 * The only constructor for 
	 * @param configuration
	 */
	@Inject
	public StackConfigurationImpl(ConfigurationProperties configuration, StackEncrypter stackEncrypter) {
		super();
		this.configuration = configuration;
		this.stackEncrypter = stackEncrypter;
	}

	/**
	 * Is this a production stack?
	 * 
	 * @return
	 */
	@Override
	public boolean isProductionStack() {
		return isProduction(getStack());
	}

	/**
	 * Does the passed stack string represent prod?
	 * 
	 * @param stack
	 * @return
	 */
	boolean isProduction(String stack) {
		return PROD.equals(stack);
	}

	/**
	 * Is this a Develop stack?
	 * 
	 * @return
	 */
	@Override
	public boolean isDevelopStack() {
		return isDevelopStack(getStack());
	}

	boolean isDevelopStack(String stack) {
		return DEV.equals(stack);
	}

	/**
	 * Is this a Hudson stack?
	 * 
	 * @return
	 */
	@Override
	public boolean isHudsonStack() {
		return isHudsonStack(getStack());
	}

	boolean isHudsonStack(String stack) {
		return HUDSON.equals(stack);
	}

	/**
	 * In production stacks the instance is numeric. In development and test stacks
	 * the instance is often the developer's name. For production stacks we need to
	 * be able to determine the order the stacks were created. For example, we
	 * staging should always have a higher number than production. This number is
	 * used to provide that order.
	 * 
	 * @return
	 */
	@Override
	public int getStackInstanceNumber() {
		return getStackInstanceNumber(getStackInstance(), isProductionStack());
	}

	/**
	 * In production stacks the instance is numeric. In development and test stacks
	 * the instance is often the developer's name. For production stacks we need to
	 * be able to determine the order the stacks were created. For example, we
	 * staging should always have a higher number than production. This number is
	 * used to provide that order.
	 * 
	 * @param instance
	 * @param isProd
	 * @return
	 */
	int getStackInstanceNumber(String instance, boolean isProd) {
		if (isProd) {
			try {
				return Integer.parseInt(instance);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"Production stacks must have a numeric stack-instance.  The number is used to determine the order the stacks were created.  Staging should always be assigned a larger stack-instance than production");
			}
		} else {
			// This is a dev or build stack
			try {
				return Integer.parseInt(instance);
			} catch (NumberFormatException e) {
				// Dev instance strings are short so they should always be in range of an
				// integer.
				// Convert the base 256 character string to a base 10 number
				return new BigInteger(instance.getBytes()).intValue();
			}
		}

	}

	/**
	 * This is the bucket for Synapse data.
	 */
	@Override
	public String getS3Bucket() {
		return configuration.getProperty("org.sagebionetworks.s3.bucket");
	}

	@Override
	public Integer getS3ReadAccessExpiryHours() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.s3.readAccessExpiryHours"));
	}

	/**
	 * This is for Attachment URLs that expire in seconds.
	 * 
	 * @return
	 */
	@Override
	public Integer getS3ReadAccessExpirySeconds() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.s3.readAccessExpirySeconds"));
	}

	public Integer getS3WriteAccessExpiryHours() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.s3.writeAccessExpiryHours"));
	}

	public String getMailPassword() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.mailPW");
	}

	/**
	 * The database connection string used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseConnectionUrl() {
		return configuration.getProperty("org.sagebionetworks.id.generator.database.connection.url");
	}

	/**
	 * The username used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseUsername() {
		return configuration.getProperty("org.sagebionetworks.id.generator.database.username");
	}

	/**
	 * The password used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabasePassword() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.id.generator.database.password");
	}

	public String getIdGeneratorDatabaseDriver() {
		return configuration.getProperty("org.sagebionetworks.id.generator.database.driver");
	}

	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseDriver() {
		return configuration.getProperty("org.sagebionetworks.repository.databaes.driver");
	}

	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getTableDatabaseDriver() {
		return configuration.getProperty("org.sagebionetworks.table.databaes.driver");
	}

	/**
	 * The repository database connection string.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseConnectionUrl() {
		// First try to load the system property
		String jdbcConnection = System.getProperty("JDBC_CONNECTION_STRING");
		if (jdbcConnection != null && !"".equals(jdbcConnection))
			return jdbcConnection;
		// Now try the environment variable
		jdbcConnection = System.getenv("JDBC_CONNECTION_STRING");
		if (jdbcConnection != null && !"".equals(jdbcConnection))
			return jdbcConnection;
		// Last try the stack configuration
		return configuration.getProperty("org.sagebionetworks.repository.database.connection.url");
	}

	/**
	 * The repository database schema name.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseSchemaName() {
		return getStack() + getStackInstance();
	}

	/**
	 * The repository database username.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseUsername() {
		return configuration.getProperty("org.sagebionetworks.repository.database.username");
	}

	/**
	 * The repository database password.
	 * 
	 * @return
	 */
	public String getRepositoryDatabasePassword() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.repository.database.password");
	}

	/**
	 * Should the connection pool connections be validated?
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolShouldValidate() {
		return configuration.getProperty("org.sagebionetworks.pool.connection.validate");
	}

	/**
	 * The SQL used to validate pool connections
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolValidateSql() {
		return configuration.getProperty("org.sagebionetworks.pool.connection.validate.sql");
	}

	/**
	 * The minimum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMinNumberConnections() {
		return configuration.getProperty("org.sagebionetworks.pool.min.number.connections");
	}

	/**
	 * The maximum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMaxNumberConnections() {
		return configuration.getProperty("org.sagebionetworks.pool.max.number.connections");
	}

	/**
	 * @return The username of the migration admin
	 */
	public String getMigrationAdminUsername() {
		return configuration.getProperty("org.sagebionetworks.migration.admin.username");
	}

	/**
	 * @return The API key of the migration admin
	 */
	public String getMigrationAdminAPIKey() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.migration.admin.apikey");
	}

	/**
	 * @return whether controller logging is enabled or not.
	 */
	public boolean getControllerLoggingEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.usage.metrics.logging.enabled"));
	}

	/**
	 * @return whether log sweeping should be enabled for this stack
	 */
	public boolean getLogSweepingEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.logging.sweeper.enabled"));
	}

	/**
	 * @return whether the log files should be deleted after they are successfully
	 *         pushed to S3
	 */
	public boolean getDeleteAfterSweepingEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.logging.sweeper.delete.enabled"));
	}

	public String getNotificationEmailSuffix() {
		return configuration.getProperty("org.sagebionetworks.notification.email.suffix");
	}

	public String getSynapseOpsEmailAddress() {
		return configuration.getProperty("org.sagebionetworks.synapseops.email.address");
	}

	/**
	 * @return whether the cloudWatch profiler should be on or off boolean. True
	 *         means on, false means off.
	 */
	public boolean getCloudWatchOnOff() {
		// Boolean toReturn =
		// Boolean.getBoolean(getProperty("org.sagebionetworks.cloud.watch.report.enabled"));
		String answer = configuration.getProperty("org.sagebionetworks.cloud.watch.report.enabled");
		boolean theValue = Boolean.parseBoolean(answer);
		return theValue;
	}

	/**
	 * @return the time in milliseconds for the cloudWatch profiler's trigger. I
	 *         till trigger and send metrics to cloudWatch ever xxx milliseconds.
	 */
	public long getCloudWatchTriggerTime() {
		return Long.valueOf(configuration.getProperty("org.sagebionetworks.cloud.watch.trigger"));
	}

	/**
	 * @return whether the call performance profiler should be on or off boolean.
	 *         True means on, false means off.
	 */
	public boolean getCallPerformanceOnOff() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.call.performance.report.enabled"));
	}

	/**
	 * @return the time in milliseconds for the call performance profiler's trigger.
	 *         It will trigger and log average call performance ever xxx
	 *         milliseconds.
	 */
	public long getCallPerformanceTriggerTime() {
		return Long.valueOf(configuration.getProperty("org.sagebionetworks.call.performance.trigger"));
	}

	/**
	 * The maximum number of threads to be used for backup/restore
	 * 
	 * @return
	 */
	public int getBackupRestoreThreadPoolMaximum() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.backup.restore.thread.pool.maximum"));
	}

	/**
	 * The maximum bytes allowed for a single query result.
	 * 
	 * @return
	 */
	public long getMaximumBytesPerQueryResult() {
		return Long.valueOf(configuration.getProperty("org.sagebionetworks.maximum.bytes.per.query.result"));
	}

	/**
	 * The maximum number entities returned in a single call
	 * 
	 * @return
	 */
	public int getMaximumNumberOfEntitiesReturnedPerCall() {
		return Integer
				.valueOf(configuration.getProperty("org.sagebionetworks.maximum.number.entities.returned.per.call"));
	}

	/**
	 * The maximum number of pixels used for a preview image width
	 * 
	 * @return
	 */
	public int getMaximumPreviewWidthPixels() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.preview.image.max.width.pixels"));
	}

	/**
	 * The maximum number of pixels used for a preview image height
	 * 
	 * @return
	 */
	public int getMaximumPreviewHeightPixels() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.preview.image.max.height.pixels"));
	}

	/**
	 * The maximum number of pixels used for an attachment image
	 * 
	 * @return
	 */
	public int getMaximumAttachmentPreviewPixels() {
		return Integer.valueOf(configuration.getProperty("org.sagebionetworks.attachment.preview.image.max.pixels"));
	}

	/**
	 * Is the search feature enabled?
	 * 
	 * @return
	 */
	public boolean getSearchEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.search.enabled"));
	}

	/**
	 * Is the DOI feature enabled?
	 * 
	 * @return
	 */
	public boolean getDoiEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.doi.enabled"));
	}

	public boolean getDoiDataciteEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.doi.datacite.enabled"));
	}


	/**
	 * The S3 Bucket for backup file. This is shared across stacks to enable data
	 * migration across a stack.
	 * 
	 * @return
	 */
	public String getSharedS3BackupBucket() {
		return getStack()+configuration.getProperty("org.sagebionetworks.shared.s3.backup.bucket");
	}

	public String getGoogleAppsOAuthAccessTokenSecret() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.bcc.googleapps.oauth.access.token.secret");
	}

	/**
	 * The AWS domain name is the <stack>+<stackInstance>
	 * 
	 * @return
	 */
	public String getAWSDomainName() {
		return getStack() + getStackInstance();
	}

	public String getWorkflowExecutionRetentionPeriodInDays() {
		return configuration.getProperty("org.sagebionetworks.swf.workflowExecutionRetentionPeriodInDays");
	}

	/**
	 * Stack and instance: <stack>-<stack_instance>
	 * 
	 * @return
	 */
	public String getStackAndStackInstancePrefix() {
		return String.format(StackConstants.STACK_AND_INSTANCE, getStack(), getStackInstance());
	}

	/**
	 * The name of the queue
	 * 
	 * @return
	 */
	public String getQueueName(String baseName) {
		return String.format(StackConstants.QUEUE_AND_TOPIC_NAME_TEMPLATE, getStack(), getStackInstance(), baseName);
	}

	/**
	 * Get the full topic name for a given object type.
	 * 
	 * @param objectType
	 * @return
	 */
	public String getRepositoryChangeTopic(String objectType) {
		return String.format(StackConstants.QUEUE_AND_TOPIC_NAME_TEMPLATE, getStack(), getStackInstance(), objectType);
	}

	/**
	 * This is the size of a single file transfer memory block used as a buffer.
	 * Note: Due to S3 limitations on the minimum size of a single part of a
	 * multi-part upload this value cannot be less 5 MB. Currently defaults to 5 MB.
	 * 
	 * @return
	 */
	public long getFileTransferBufferSizeBytes() {
		return Long.parseLong(
				configuration.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.buffer.bytes"));
	}

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFileTransferMemoryPercentOfMax() {
		return Double.parseDouble(
				configuration.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.percent.of.max"));
	}

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFilePreivewMemoryPercentOfMax() {
		return Double.parseDouble(
				configuration.getProperty("org.sagebionetworks.repo.manager.file.preview.memory.percent.of.max"));
	}

	/**
	 * Validate that fileTransferMemoryPercentOfMax + filePreivewMemoryPercentOfMax
	 * does not exceed 90%
	 */
	private void validateFileMemoryPercentages() {
		double transferPercent = getFileTransferMemoryPercentOfMax();
		double previewPercent = getFilePreivewMemoryPercentOfMax();
		if (transferPercent + previewPercent > 0.9)
			throw new IllegalArgumentException(
					"file.transfer.memory.percent.of.max + file.preview.memory.percent.of.max excceds 0.9 (90%)");
	}

	/**
	 * This is the maximum memory used by file transfer memory pool. Currently
	 * defaults to 70% of max memory.
	 * 
	 * @return
	 */
	public long getMaxFileTransferMemoryPoolBytes() {
		// This is a function of the
		validateFileMemoryPercentages();
		double transferPercent = getFileTransferMemoryPercentOfMax();
		// Get the max
		return (long) (Runtime.getRuntime().maxMemory() * transferPercent);
	}

	/**
	 * The maximum memory that can be used for preview generation.
	 * 
	 * @return
	 */
	public long getMaxFilePreviewMemoryPoolBytes() {
		// This is a function of the
		validateFileMemoryPercentages();
		double previewPercent = getFilePreivewMemoryPercentOfMax();
		// Get the max
		return (long) (Runtime.getRuntime().maxMemory() * previewPercent);
	}

	/**
	 * Should messages be published to the AWS topic?
	 * 
	 * @return
	 */
	public boolean getShouldMessagesBePublishedToTopic() {
		return Boolean.parseBoolean(
				configuration.getProperty("org.sagebionetworks.repo.manage.shouldMessagesBePublishedToTopic"));
	}

	/**
	 * Prefix under which DOIs should be registered.
	 */
	public String getDoiPrefix() {
		if (isProductionStack()) {
			return configuration.getProperty("org.sagebionetworks.doi.prefix");
		} else {
			// We change the prefix to prevent collisions (separate developer builds may have objects with the same DOI)
			return configuration.getProperty("org.sagebionetworks.doi.prefix") + "/" + getStackInstance();
		}
	}

	/**
	 * Datacite user name.
	 */
	public String getDataciteUsername() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.doi.datacite.username");
	}

	/**
	 * Datacite password.
	 */
	public String getDatacitePassword() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.doi.datacite.password");
	}

	/**
	 * Endpoint for DataCite's DOI minting API
	 */
	public String getDataciteAPIEndpoint() {
		return configuration.getProperty("org.sagebionetworks.doi.datacite.api.endpoint");
	}

	/**
	 * The maximum size of a backup batch.
	 * 
	 * @return
	 */
	public Long getMigrationBackupBatchMax() {
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.repo.manager.migration.backup.batch.max"));
	}

	/**
	 * This should match the Database max_allowed_packet value. See PLFM-1900
	 * 
	 * @return
	 */
	public Integer getMigrationMaxAllowedPacketBytes() {
		return Integer.parseInt(
				configuration.getProperty("org.sagebionetworks.repo.model.dbo.migration.max.allowed.packet.byte"));
	}

	public Integer getSemaphoreGatedMaxRunnersTableCluster() {
		return Integer
				.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.table.cluster"));
	}

	/**
	 * The maximum timeout for an exclusive lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreExclusiveMaxTimeoutMS() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.exclusive.max.timeout.ms"));
	}

	/**
	 * The maximum timeout for a shared lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreSharedMaxTimeoutMS() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.shared.max.timeout.ms"));
	}

	@Override
	public Integer getWriteReadSemaphoreRunnerMaxReaders(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.write.read.semaphore.runner.max.readers"));
	}

	/**
	 * This is the maximum amount of time the upload workers are allowed to take
	 * before timing out.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonTimeoutMS() {
		return Long.parseLong(
				configuration.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.timeout.ms"));
	}

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonMainMaxThreads() {
		return Long.parseLong(configuration
				.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.main.max.threads"));
	}

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons copy part sub-task.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonCopyPartMaxThreads() {
		return Long.parseLong(configuration
				.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.copy.part.max.threads"));
	}

	/**
	 * Get credentials for the Jira service account used to create Jira issues
	 * 
	 * @return
	 */
	public String getJiraUserEmail() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.repo.manager.jira.user.email");
	}

	public String getJiraUserApikey() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.repo.manager.jira.user.apikey");
	}

	/**
	 * Entity path for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityPath() {
		return configuration.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.path");
	}

	/**
	 * Entity ID for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityId() {
		return configuration.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.id");
	}

	/**
	 * Entity path for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityPath() {
		return configuration.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.path");
	}

	/**
	 * Entity ID for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityId() {
		return configuration.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.id");
	}

	/**
	 * Get the name of the table row bucket.
	 * 
	 * @return
	 */
	public String getTableRowChangeBucketName() {
		return String.format(StackConstants.TABLE_ROW_CHANGE_BUCKET, getStack());
	}
	
	/**
	 * The name of the bucket for view snapshots
	 * @return
	 */
	public String getViewSnapshotBucketName() {
		return String.format(StackConstants.VIEW_SNAPSHOT_BUCKET, getStack());
	}

	/**
	 * 
	 * @return
	 */
	public String getOAuth2GoogleClientId() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.oauth2.google.client.id");
	}

	/**
	 * 
	 * @return
	 */
	public String getOAuth2GoogleClientSecret() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.oauth2.google.client.secret");
	}

	/**
	 * 
	 * @return
	 */
	public String getOAuth2ORCIDClientId() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.oauth2.orcid.client.id");
	}

	/**
	 * 
	 * @return
	 */
	public String getOAuth2ORCIDClientSecret() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.oauth2.orcid.client.secret");
	}

	/**
	 * Get the max bytes per HTTP request for a table.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerRequest() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.table.max.bytes.per.request"));
	}

	/**
	 * The maximum number of rows in a single table change set file.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerChangeSet() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.table.max.bytes.per.change.set"));
	}

	/**
	 * Get the max bytes per HTTP request for a table.
	 * 
	 * @return
	 */
	public int getTableMaxEnumValues() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.table.max.enum.values"));
	}

	/**
	 * The maximum amount of time in MS that the table worker can hold the semaphore
	 * lock on the table.
	 * 
	 * @return
	 */
	public long getTableWorkerTimeoutMS() {
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.table.worker.timeout.ms"));
	}

	/**
	 * The maxiumn amount of time in MS that a table reader can hold a read lock on
	 * a table.
	 * 
	 * @return
	 */
	public long getTableReadTimeoutMS() {
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.table.read.timeout.ms"));
	}

	public Integer getMaxConcurrentRepoConnections() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.max.concurrent.repo.connections"));
	}

	/**
	 * The amount of time (MS) the ChangeSentMessageSynchWorker sleeps between
	 * pages.
	 * 
	 * @return
	 */
	public Long getChangeSynchWorkerSleepTimeMS() {
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.worker.change.synch.sleep.ms"));
	}

	/**
	 * The minium page size used by ChangeSentMessageSynchWorker.
	 * 
	 * @return
	 */
	public Integer getChangeSynchWorkerMinPageSize() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.worker.change.synch.min.page.size"));
	}

	/**
	 * Get the name of the audit access record bucket.
	 * 
	 * @return
	 */
	public String getAuditRecordBucketName() {
		return String.format(StackConstants.ACCESS_RECORD_BUCKET, getStack());
	}

	/**
	 * Get the name of the object snapshot record bucket.
	 * 
	 * @return
	 */
	public String getSnapshotRecordBucketName() {
		return String.format(StackConstants.SNAPSHOT_RECORD_BUCKET, getStack());
	}

	/**
	 * Get the name of the object snapshot record bucket.
	 * 
	 * @return
	 */
	public String getDiscussionBucketName() {
		return String.format(StackConstants.DISCUSSION_BUCKET, getStack());
	}

	/**
	 * Get the name of the stack log bucket.
	 * 
	 * @return
	 */
	public String getLogBucketName() {
		return String.format(StackConstants.STACK_LOG_BUCKET, getStack());
	}

	/**
	 * Get the name of the stack test bucket.
	 * 
	 * @return
	 */
	public String getExternalS3TestBucketName() {
		return String.format(StackConstants.EXTERNAL_S3_TEST_BUCKET, getStack());
	}

	/**
	 * Get the number of database in the table's cluster.
	 * 
	 * @return
	 */
	public int getTablesDatabaseCount() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.table.cluster.database.count"));
	}

	/**
	 * Get the endpoint of a table's database given its index.
	 * 
	 * @param index
	 *            Each database in the cluster has an index: 0 - n-1.
	 * @return
	 */
	public String getTablesDatabaseEndpointForIndex(int index) {
		return configuration.getProperty("org.sagebionetworks.table.cluster.endpoint." + index);
	}

	/**
	 * Get the schema name of a table's database given its index.
	 * 
	 * @param index
	 *            Each database in the cluster has an index: 0 - n-1.
	 * @return
	 * @param index
	 * @return
	 */
	public String getTablesDatabaseSchemaForIndex(int index) {
		return configuration.getProperty("org.sagebionetworks.table.cluster.schema." + index);
	}

	/**
	 * @return for dev stacks, this controls whether emails are delivered or sent to
	 *         a file (the default)
	 */
	public boolean getDeliverEmail() {
		String emailDeliveredString = null;
		try {
			emailDeliveredString = configuration.getProperty("org.sagebionetworks.email.delivered");
		} catch (IllegalArgumentException e) {
			emailDeliveredString = null;
		}
		if (emailDeliveredString == null || emailDeliveredString.length() == 0)
			return false;
		return Boolean.parseBoolean(emailDeliveredString);
	}

	/*
	 * Credentials used by CloudMailIn to send authenticated requests to the repo
	 * services.
	 */
	public String getCloudMailInUser() {
		return getServiceAuthKey(SERVICE_CLOUDMAILIN);
	}

	/*
	 * Credentials used by CloudMailIn to send authenticated requests to the repo
	 * services.
	 */
	public String getCloudMailInPassword() {
		return getServiceAuthSecret(SERVICE_CLOUDMAILIN);
	}

	public String getDefaultPortalNotificationEndpoint() {
		return configuration.getProperty("org.sagebionetworks.notification.portal.endpoint");
	}

	public String getDefaultPortalProfileSettingEndpoint() {
		return configuration.getProperty("org.sagebionetworks.profile.setting.portal.endpoint");
	}

	/*
	 * Credentials used by Docker Registry to send events to the repo services.
	 */
	public String getDockerRegistryUser() {
		return getServiceAuthKey(SERVICE_DOCKER_REGISTRY);
	}

	/*
	 * Credentials used by Docker Registry to send events to the repo services.
	 */
	public String getDockerRegistryPassword() {
		return getServiceAuthSecret(SERVICE_DOCKER_REGISTRY);
	}

	/**
	 * Credentials for signing Docker authorization bearer tokens
	 */
	public String getDockerAuthorizationPrivateKey() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.docker.authorization.private.key");
	}

	public String getDockerAuthorizationCertificate() {
		return stackEncrypter.getDecryptedProperty("org.sagebionetworks.docker.authorization.certificate");
	}

	/**
	 * Credentials for signing OIDC JSON Web Tokens
	 */
	public List<String> getOIDCSignatureRSAPrivateKeys() {
		String s = stackEncrypter.getDecryptedProperty("org.sagebionetworks.oidc.signature.rsa.private.keys");
		s = s.replaceAll("\\s+", "");
		return Arrays.asList(s.split(","));
	}

	public List<String> getDockerRegistryHosts() {
		String s = configuration.getProperty("org.sagebionetworks.docker.registry.hostnames");
		s = s.replaceAll("\\s+", "");
		return Arrays.asList(s.split(","));
	}

	public List<String> getDockerReservedRegistryHosts() {
		String s = configuration.getProperty("org.sagebionetworks.docker.reserved.hostnames");
		s = s.replaceAll("\\s+", "");
		return Arrays.asList(s.split(","));
	}

	/**
	 * 
	 * @return if missing or false then certified user restrictions are in effect.
	 *         Setting to true disables.
	 */
	public Boolean getDisableCertifiedUser() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.notification.portal.endpoint"));
	}

	/**
	 * Are users allowed to create entities of the old types?
	 */
	public boolean getAllowCreationOfOldEntities() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.allow.create.old.entities"));
	}

	/**
	 * Are users allowed to create old attachments (entity attachments and user
	 * profile attachments?)
	 */
	public boolean getAllowCreationOfOldAttachments() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.allow.create.old.attachments"));
	}

	/**
	 * @return the markdown service endpoint
	 */
	public String getMarkdownServiceEndpoint() {
		return configuration.getProperty("org.sagebionetworks.markdown.service.endpoint");
	}

	/**
	 * @return the Synapse base URL
	 */
	public String getSynapseBaseUrl() {
		return configuration.getProperty("org.sagebionetworks.synapse.base.url");
	}

	/**
	 * The maximum number of entities per container.
	 * 
	 * @return
	 */
	public Long getMaximumNumberOfEntitiesPerContainer() {
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.synapse.max.entities.per.container"));
	}

	/**
	 * Stack identifies production vs develop.
	 * 
	 * @return Will be 'prod' for production or 'dev' for develop.
	 */
	public String getStack() {
		return configuration.getProperty(StackConstants.STACK_PROPERTY_NAME);
	}

	/**
	 * The instance number of this stack. Can also be a developer's name for a 'dev'
	 * stack.
	 * 
	 * @return
	 */
	public String getStackInstance() {
		return configuration.getProperty(StackConstants.STACK_INSTANCE_PROPERTY_NAME);
	}

	/**
	 * 
	 * @return authentication service private endpoint
	 */
	public String getAuthenticationServicePrivateEndpoint() {
		return configuration.getProperty("org.sagebionetworks.authenticationservice.privateendpoint");
	}

	/**
	 * @return authentication service public endpoint
	 */
	public String getAuthenticationServicePublicEndpoint() {
		return configuration.getProperty("org.sagebionetworks.authenticationservice.publicendpoint");
	}

	/**
	 * @return repository service endpoint
	 */
	public String getRepositoryServiceEndpoint() {
		return configuration.getProperty("org.sagebionetworks.repositoryservice.endpoint");
	}

	/**
	 * Get the file service Endpoint.
	 * 
	 * @return
	 */
	public String getFileServiceEndpoint() {
		return configuration.getProperty("org.sagebionetworks.fileservice.endpoint");
	}

	/**
	 * 
	 * @return search service endpoint
	 */
	public String getSearchServiceEndpoint() {
		return configuration.getProperty("org.sagebionetworks.searchservice.endpoint");
	}

	/**
	 * 
	 * @return docker service endpoint
	 */
	public String getDockerServiceEndpoint() {
		return configuration.getProperty("org.sagebionetworks.docker.endpoint");
	}

	/**
	 * 
	 * @return the endpoint for the docker registry event listener
	 */
	public String getDockerRegistryListenerEndpoint() {
		return configuration.getProperty("org.sagebionetworks.docker.registry.listener.endpoint");
	}

	/**
	 * Get the decrypted HMAC signing key for a given version.
	 * 
	 * @param keyVersion
	 * @return
	 */
	public String getHmacSigningKeyForVersion(int keyVersion) {
		StringJoiner joiner = new StringJoiner(".");
		joiner.add("org.sagebionetworks.hmac.signing.key.version");
		joiner.add("" + keyVersion);
		String key = joiner.toString();
		return stackEncrypter.getDecryptedProperty(key);
	}

	/**
	 * Get the current version of the HMAC signing key to be used to sign all new
	 * requests.
	 * 
	 * @return
	 */
	public int getCurrentHmacSigningKeyVersion() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.hmac.signing.key.current.version"));
	}

	@Override
	public boolean getGoogleCloudEnabled() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.google.cloud.enabled"));
	}

	@Override
	public String getDecodedGoogleCloudServiceAccountCredentials() {
		// The credentials should be passed in with base64 encoding
		return new String(
				Base64.getDecoder().decode(
						stackEncrypter.getDecryptedProperty("org.sagebionetworks.google.cloud.key")
								.getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8);
	}

	@Override
	public boolean useSSLConnectionForTablesDatabase() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.table.cluster.use.ssl"));
	}

	@Override
	public String getOAuthAuthorizationEndpoint() {
		return configuration.getProperty("org.sagebionetworks.oauth.authorization.endpoint");
	}
	
	@Override
	public int getMaximumMonthsForMonthlyStatistics() {
		return  Integer.parseInt(configuration.getProperty("org.sagebionetworks.statistics.monthly.max"));
	}

	@Override
	public String getTempCredentialsIamRoleArn() {
		if (configuration.hasProperty(CONFIG_KEY_STS_IAM_ARN)) {
			return configuration.getProperty(CONFIG_KEY_STS_IAM_ARN);
		} else {
			return null;
		}
	}
	
	@Override
	public String getServiceAuthKey(String serviceName) {
		return stackEncrypter.getDecryptedProperty(String.format(SERVICE_AUTH_TEMPLATE, serviceName, "key"));
	}
	
	@Override
	public String getServiceAuthSecret(String serviceName) {
		return stackEncrypter.getDecryptedProperty(String.format(SERVICE_AUTH_TEMPLATE, serviceName, "secret"));
	}
	
	@Override
	public String getRepositoryServiceProdEndpoint() {
		return configuration.getProperty("org.sagebionetworks.repositoryservice.endpoint.prod");
	}
	
	@Override
	public Long getKinesisMaxRetryDelay() {
		if (configuration.hasProperty("org.sagebionetworks.kinesis.maxRetryDelay")) {
			return Long.parseLong(configuration.getProperty("org.sagebionetworks.kinesis.maxRetryDelay"));
		}
		return null;
	}
}
