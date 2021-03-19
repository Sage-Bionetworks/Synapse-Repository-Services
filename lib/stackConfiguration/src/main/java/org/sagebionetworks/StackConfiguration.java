package org.sagebionetworks;

import java.util.List;

/**
 * 
 * Provides access to stack configuration information.
 *
 */
public interface StackConfiguration {
	
	String SERVICE_CLOUDMAILIN = "cloudmailin";
	String SERVICE_DOCKER_REGISTRY = "docker.registry";
	String SERVICE_ADMIN = "admin";

	/**
	 * Is this a production stack?
	 * 
	 * @return
	 */
	public boolean isProductionStack();

	/**
	 * Is this a Develop stack?
	 * 
	 * @return
	 */
	public boolean isDevelopStack();

	/**
	 * Is this a Hudson stack?
	 * 
	 * @return
	 */
	public boolean isHudsonStack();

	/**
	 * In production stacks the instance is numeric. In development and test stacks
	 * the instance is often the developer's name. For production stacks we need to
	 * be able to determine the order the stacks were created. For example, we
	 * staging should always have a higher number than production. This number is
	 * used to provide that order.
	 * 
	 * @return
	 */
	public int getStackInstanceNumber();

	/**
	 * This is the bucket for Synapse data.
	 */
	public String getS3Bucket();

	public Integer getS3ReadAccessExpiryHours();

	/**
	 * This is for Attachment URLs that expire in seconds.
	 * 
	 * @return
	 */
	public Integer getS3ReadAccessExpirySeconds();

	public Integer getS3WriteAccessExpiryHours();

	public String getMailPassword();

	/**
	 * The database connection string used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseConnectionUrl();

	/**
	 * The username used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseUsername();

	/**
	 * The password used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabasePassword();

	public String getIdGeneratorDatabaseDriver();

	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseDriver();

	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getTableDatabaseDriver();

	/**
	 * The repository database connection string.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseConnectionUrl();

	/**
	 * The repository database schema name.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseSchemaName();

	/**
	 * The repository database username.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseUsername();

	/**
	 * The repository database password.
	 * 
	 * @return
	 */
	public String getRepositoryDatabasePassword();

	/**
	 * Should the connection pool connections be validated?
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolShouldValidate();

	/**
	 * The SQL used to validate pool connections
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolValidateSql();

	/**
	 * The minimum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMinNumberConnections();

	/**
	 * The maximum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMaxNumberConnections();

	/**
	 * @return The username of the migration admin
	 */
	public String getMigrationAdminUsername();

	/**
	 * @return The API key of the migration admin
	 */
	public String getMigrationAdminAPIKey();

	/**
	 * @return whether controller logging is enabled or not.
	 */
	public boolean getControllerLoggingEnabled();

	/**
	 * @return whether log sweeping should be enabled for this stack
	 */
	public boolean getLogSweepingEnabled();

	/**
	 * @return whether the log files should be deleted after they are successfully
	 *         pushed to S3
	 */
	public boolean getDeleteAfterSweepingEnabled();

	public String getNotificationEmailSuffix();

	public String getSynapseOpsEmailAddress();

	/**
	 * @return whether the cloudWatch profiler should be on or off boolean. True
	 *         means on, false means off.
	 */
	public boolean getCloudWatchOnOff();

	/**
	 * @return the time in milliseconds for the cloudWatch profiler's trigger. I
	 *         till trigger and send metrics to cloudWatch ever xxx milliseconds.
	 */
	public long getCloudWatchTriggerTime();

	/**
	 * @return whether the call performance profiler should be on or off boolean.
	 *         True means on, false means off.
	 */
	public boolean getCallPerformanceOnOff();

	/**
	 * @return the time in milliseconds for the call performance profiler's trigger.
	 *         It will trigger and log average call performance ever xxx
	 *         milliseconds.
	 */
	public long getCallPerformanceTriggerTime();

	/**
	 * The maximum number of threads to be used for backup/restore
	 * 
	 * @return
	 */
	public int getBackupRestoreThreadPoolMaximum();

	/**
	 * The maximum bytes allowed for a single query result.
	 * 
	 * @return
	 */
	public long getMaximumBytesPerQueryResult();

	/**
	 * The maximum number entities returned in a single call
	 * 
	 * @return
	 */
	public int getMaximumNumberOfEntitiesReturnedPerCall();

	/**
	 * The maximum number of pixels used for a preview image width
	 * 
	 * @return
	 */
	public int getMaximumPreviewWidthPixels();

	/**
	 * The maximum number of pixels used for a preview image height
	 * 
	 * @return
	 */
	public int getMaximumPreviewHeightPixels();

	/**
	 * The maximum number of pixels used for an attachment image
	 * 
	 * @return
	 */
	public int getMaximumAttachmentPreviewPixels();

	/**
	 * Is the search feature enabled?
	 * 
	 * @return
	 */
	public boolean getSearchEnabled();

	/**
	 * Is the DOI feature enabled?
	 * 
	 * @return
	 */
	public boolean getDoiEnabled();

	public boolean getDoiDataciteEnabled();

	/**
	 * The S3 Bucket for backup file. This is shared across stacks to enable data
	 * migration across a stack.
	 * 
	 * @return
	 */
	public String getSharedS3BackupBucket();

	public String getGoogleAppsOAuthAccessTokenSecret();

	/**
	 * The AWS domain name is the <stack>+<stackInstance>
	 * 
	 * @return
	 */
	public String getAWSDomainName();

	public String getWorkflowExecutionRetentionPeriodInDays();

	/**
	 * Stack and instance: <stack>-<stack_instance>
	 * 
	 * @return
	 */
	public String getStackAndStackInstancePrefix();

	/**
	 * The name of the async queue
	 * 
	 * @return
	 */
	public String getQueueName(String baseName);

	/**
	 * Get the full topic name for a given object type.
	 * 
	 * @param objectType
	 * @return
	 */
	public String getRepositoryChangeTopic(String objectType);

	/**
	 * This is the size of a single file transfer memory block used as a buffer.
	 * Note: Due to S3 limitations on the minimum size of a single part of a
	 * multi-part upload this value cannot be less 5 MB. Currently defaults to 5 MB.
	 * 
	 * @return
	 */
	public long getFileTransferBufferSizeBytes();

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFileTransferMemoryPercentOfMax();

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFilePreivewMemoryPercentOfMax();

	/**
	 * This is the maximum memory used by file transfer memory pool. Currently
	 * defaults to 70% of max memory.
	 * 
	 * @return
	 */
	public long getMaxFileTransferMemoryPoolBytes();

	/**
	 * The maximum memory that can be used for preview generation.
	 * 
	 * @return
	 */
	public long getMaxFilePreviewMemoryPoolBytes();

	/**
	 * Should messages be published to the AWS topic?
	 * 
	 * @return
	 */
	public boolean getShouldMessagesBePublishedToTopic();

	/**
	 * DataCite user name.
	 */
	public String getDataciteUsername();

	/**
	 * DataCite password.
	 */
	public String getDatacitePassword();

	/**
	 * Endpoint for DataCite's DOI minting API
	 */
	public String getDataciteAPIEndpoint();

	/**
	 * Prefix under which DOIs should be registered. DOI prefix.
	 */
	public String getDoiPrefix();

	/**
	 * The maximum size of a backup batch.
	 * 
	 * @return
	 */
	public Long getMigrationBackupBatchMax();

	/**
	 * This should match the Database max_allowed_packet value. See PLFM-1900
	 * 
	 * @return
	 */
	public Integer getMigrationMaxAllowedPacketBytes();

	public Integer getSemaphoreGatedMaxRunnersTableCluster();

	/**
	 * The maximum timeout for an exclusive lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreExclusiveMaxTimeoutMS();

	/**
	 * The maximum timeout for a shared lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreSharedMaxTimeoutMS();

	/**
	 * Maximum number of reader locks allowed at the same time for WriteReadSemaphoreRunner
	 * @return
	 */
	public Integer getWriteReadSemaphoreRunnerMaxReaders();

	/**
	 * This is the maximum amount of time the upload workers are allowed to take
	 * before timing out.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonTimeoutMS();

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonMainMaxThreads();

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons copy part sub-task.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonCopyPartMaxThreads();

	/**
	 * Get credentials for the Jira service account used to create Jira issues
	 * 
	 * @return
	 */
	public String getJiraUserEmail();

	public String getJiraUserApikey();

	/**
	 * Entity path for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityPath();

	/**
	 * Entity ID for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityId();

	/**
	 * Entity path for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityPath();

	/**
	 * Entity ID for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityId();

	/**
	 * Get the name of the table row bucket.
	 * 
	 * @return
	 */
	public String getTableRowChangeBucketName();
	
	/**
	 * S3 bucket for view snapshots.
	 * @return
	 */
	public String getViewSnapshotBucketName();

	/**
	 * 
	 * @return
	 */
	public String getOAuth2GoogleClientId();

	/**
	 * 
	 * @return
	 */
	public String getOAuth2GoogleClientSecret();

	/**
	 * 
	 * @return
	 */
	public String getOAuth2ORCIDClientId();

	/**
	 * 
	 * @return
	 */
	public String getOAuth2ORCIDClientSecret();

	/**
	 * Get the max bytes per HTTP request for a table.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerRequest();

	/**
	 * The maximum number of rows in a single table change set file.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerChangeSet();

	/**
	 * Get the max bytes per HTTP request for a table.
	 * 
	 * @return
	 */
	public int getTableMaxEnumValues();

	/**
	 * The maximum amount of time in MS that the table worker can hold the semaphore
	 * lock on the table.
	 * 
	 * @return
	 */
	public long getTableWorkerTimeoutMS();

	/**
	 * The maxiumn amount of time in MS that a table reader can hold a read lock on
	 * a table.
	 * 
	 * @return
	 */
	public long getTableReadTimeoutMS();

	public Integer getMaxConcurrentRepoConnections();

	/**
	 * The amount of time (MS) the ChangeSentMessageSynchWorker sleeps between
	 * pages.
	 * 
	 * @return
	 */
	public Long getChangeSynchWorkerSleepTimeMS();

	/**
	 * The minium page size used by ChangeSentMessageSynchWorker.
	 * 
	 * @return
	 */
	public Integer getChangeSynchWorkerMinPageSize();

	/**
	 * Get the name of the audit access record bucket.
	 * 
	 * @return
	 */
	public String getAuditRecordBucketName();

	/**
	 * Get the name of the object snapshot record bucket.
	 * 
	 * @return
	 */
	public String getSnapshotRecordBucketName();

	/**
	 * Get the name of the object snapshot record bucket.
	 * 
	 * @return
	 */
	public String getDiscussionBucketName();

	/**
	 * Get the name of the stack log bucket.
	 * 
	 * @return
	 */
	public String getLogBucketName();

	/**
	 * Get the name of the stack test bucket.
	 * 
	 * @return
	 */
	public String getExternalS3TestBucketName();

	/**
	 * Get the number of database in the table's cluster.
	 * 
	 * @return
	 */
	public int getTablesDatabaseCount();

	/**
	 * Get the endpoint of a table's database given its index.
	 * 
	 * @param index
	 *            Each database in the cluster has an index: 0 - n-1.
	 * @return
	 */
	public String getTablesDatabaseEndpointForIndex(int index);

	/**
	 * Get the schema name of a table's database given its index.
	 * 
	 * @param index
	 *            Each database in the cluster has an index: 0 - n-1.
	 * @return
	 * @param index
	 * @return
	 */
	public String getTablesDatabaseSchemaForIndex(int index);
	
	/**
	 * Should an SSL connection be used when connecting to the table's database?
	 * @return
	 */
	public boolean useSSLConnectionForTablesDatabase();

	/**
	 * @return for dev stacks, this controls whether emails are delivered or sent to
	 *         a file (the default)
	 */
	public boolean getDeliverEmail();

	/*
	 * Credentials used by CloudMailIn to send authenticated requests to the repo
	 * services.
	 */
	public String getCloudMailInUser();

	/*
	 * Credentials used by CloudMailIn to send authenticated requests to the repo
	 * services.
	 */
	public String getCloudMailInPassword();

	public String getDefaultPortalNotificationEndpoint();

	public String getDefaultPortalProfileSettingEndpoint();

	/*
	 * Credentials used by Docker Registry to send events to the repo services.
	 */
	public String getDockerRegistryUser();

	/*
	 * Credentials used by Docker Registry to send events to the repo services.
	 */
	public String getDockerRegistryPassword();

	/**
	 * Credentials for signing Docker authorization bearer tokens
	 */
	public String getDockerAuthorizationPrivateKey();

	public String getDockerAuthorizationCertificate();

	/**
	 * Credentials for signing OIDC JSON Web Tokens
	 */
	public List<String> getOIDCSignatureRSAPrivateKeys();
	
	public List<String> getDockerRegistryHosts();

	public List<String> getDockerReservedRegistryHosts();

	/**
	 * 
	 * @return if missing or false then certified user restrictions are in effect.
	 *         Setting to true disables.
	 */
	public Boolean getDisableCertifiedUser();

	/**
	 * Are users allowed to create entities of the old types?
	 */
	public boolean getAllowCreationOfOldEntities();

	/**
	 * Are users allowed to create old attachments (entity attachments and user
	 * profile attachments?)
	 */
	public boolean getAllowCreationOfOldAttachments();

	/**
	 * @return the markdown service endpoint
	 */
	public String getMarkdownServiceEndpoint();

	/**
	 * @return the Synapse base URL
	 */
	public String getSynapseBaseUrl();

	/**
	 * The maximum number of entities per container.
	 * 
	 * @return
	 */
	public Long getMaximumNumberOfEntitiesPerContainer();

	/**
	 * Stack identifies production vs develop.
	 * 
	 * @return Will be 'prod' for production or 'dev' for develop.
	 */
	public String getStack();

	/**
	 * The instance number of this stack. Can also be a developer's name for a 'dev'
	 * stack.
	 * 
	 * @return
	 */
	public String getStackInstance();

	/**
	 * 
	 * @return authentication service private endpoint
	 */
	public String getAuthenticationServicePrivateEndpoint();

	/**
	 * @return authentication service public endpoint
	 */
	public String getAuthenticationServicePublicEndpoint();

	/**
	 * @return repository service endpoint
	 */
	public String getRepositoryServiceEndpoint();

	/**
	 * Get the file service Endpoint.
	 * 
	 * @return
	 */
	public String getFileServiceEndpoint();

	/**
	 * 
	 * @return search service endpoint
	 */
	public String getSearchServiceEndpoint();

	/**
	 * 
	 * @return docker service endpoint
	 */
	public String getDockerServiceEndpoint();

	/**
	 * 
	 * @return the endpoint for the docker registry event listener
	 */
	public String getDockerRegistryListenerEndpoint();

	/**
	 * Get the decrypted HMAC signing key for a given version.
	 * 
	 * @param keyVersion
	 * @return
	 */
	public String getHmacSigningKeyForVersion(int keyVersion);

	/**
	 * Get the current version of the HMAC signing key to be used to sign all new
	 * requests.
	 * 
	 * @return
	 */
	public int getCurrentHmacSigningKeyVersion();

	/**
	 * Get whether Google Cloud features should be enabled or not.
	 * @return
	 */
	public boolean getGoogleCloudEnabled();

	/**
	 * Get the credentials for a Google Cloud service account.
	 *
	 * @return
	 */
	public String getDecodedGoogleCloudServiceAccountCredentials();
	
	/**
	 * Get the authorization endpoint that Synapse OAuth 2.0 clients will redirect 
 	 * the browser to, to prompt the user to authorize that client.
 	 * 
	 * @return
	 */
	public String getOAuthAuthorizationEndpoint();
	
	/**
	 * @return The maximum number of months to process for monthly statistics
	 */
	public int getMaximumMonthsForMonthlyStatistics();

	/**
	 * The ARN for the IAM Role that the StsManager uses. We call AssumeRole on this ARN to generate the temporary S3
	 * credentials that we pass to the caller.
	 */
	String getTempCredentialsIamRoleArn();
	
	/**
	 * @param serviceName Name of the service
	 * @return The key used to authenticate the service with the given name
	 */
	String getServiceAuthKey(String serviceName);
	
	/**
	 * @param serviceName Name of the service
	 * @return The secret used to authenticate the service with the given name
	 */
	String getServiceAuthSecret(String serviceName);
	
	/**
	 * @return The configured endpoint for the repository services prod stack
	 */
	String getRepositoryServiceProdEndpoint();
	
	/**
	 * @return Max amount of time in ms that a kinesis delivery retry can wait for, can be null 
	 */
	Long getKinesisMaxRetryDelay();
	
}
