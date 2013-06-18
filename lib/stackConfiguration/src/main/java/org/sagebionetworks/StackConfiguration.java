package org.sagebionetworks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * StackConfiguration wraps all configuration needed for a Synapse service stack
 * and exposes it via this programmatic API.
 * 
 * Note that it wraps an instance of TemplatedConfigurationImpl which was
 * initialized with a template for the properties a Synapse service stack should
 * have and default property values for service stacks.
 * 
 */
public class StackConfiguration {

	static final String DEFAULT_PROPERTIES_FILENAME = "/stack.properties";
	static final String TEMPLATE_PROPERTIES = "/template.properties";

	private static final Logger log = Logger.getLogger(StackConfiguration.class
			.getName());

	private static TemplatedConfiguration configuration = null;
	private static InetAddress address = null; 

	static {
		init();
	}

	public static void init() {
		configuration = new TemplatedConfigurationImpl(DEFAULT_PROPERTIES_FILENAME,
				TEMPLATE_PROPERTIES);
		// Load the stack configuration the first time this class is referenced
		try {
			configuration.reloadConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}
	}

	public static void reloadStackConfiguration() {
		configuration.reloadConfiguration();
	}

	/**
	 * The name of the stack.
	 * 
	 * @return
	 */
	public static String getStack() {
		return configuration.getStack();
	}

	/**
	 * The stack instance (i.e 'A', or 'B')
	 * 
	 * @return
	 */
	public static String getStackInstance() {
		return configuration.getStackInstance();
	}

	/**
	 * @return the encryption key for this stack
	 */
	public static String getEncryptionKey() {
		return configuration.getEncryptionKey();
	}

	/**
	 * Get the IAM user ID (Access Key ID)
	 * 
	 * @return
	 */
	public static String getIAMUserId() {
		return configuration.getIAMUserId();
	}

	/**
	 * Get the IAM user Key (Secret Access Key)
	 * 
	 * @return
	 */
	public static String getIAMUserKey() {
		return configuration.getIAMUserKey();
	}

	public static String getAuthenticationServicePrivateEndpoint() {
		return configuration.getAuthenticationServicePrivateEndpoint();
	}

	public static String getAuthenticationServicePublicEndpoint() {
		return configuration.getAuthenticationServicePublicEndpoint();
	}

	public static String getRepositoryServiceEndpoint() {
		return configuration.getRepositoryServiceEndpoint();
	}
	
	public static String getFileServiceEndpoint() {
		return configuration.getFileServiceEndpoint();
	}
	
	public static String getCrowdEndpoint() {
		return configuration.getProperty("org.sagebionetworks.crowd.endpoint");
	}

	/**
	 * This is the bucket for workflow-related files such as configuration or
	 * search document files. Each workflow should store stuff under its own
	 * workflow name prefix so that we can configure permissions not only on the
	 * bucket but also the S3 object prefix.
	 */
	public static String getS3WorkflowBucket() {
		return configuration
				.getProperty("org.sagebionetworks.s3.bucket.workflow");
	}

	/**
	 * This is the bucket for Synapse data.
	 */
	public static String getS3Bucket() {
		return configuration.getProperty("org.sagebionetworks.s3.bucket");
	}

	public static Integer getS3ReadAccessExpiryHours() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.s3.readAccessExpiryHours"));
	}
	
	/**
	 * This is for Attachment URLs that expire in seconds.
	 * @return
	 */
	public static Integer getS3ReadAccessExpirySeconds() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.s3.readAccessExpirySeconds"));
	}

	public static Integer getS3WriteAccessExpiryHours() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.s3.writeAccessExpiryHours"));
	}

	public static String getCrowdAPIApplicationKey() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.crowdApplicationKey");
	}

	public static String getMailPassword() {
		return configuration.getDecryptedProperty("org.sagebionetworks.mailPW");
	}

	/**
	 * The database connection string used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseConnectionUrl() {
		return configuration
				.getProperty("org.sagebionetworks.id.generator.database.connection.url");
	}

	/**
	 * The username used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabaseUsername() {
		return configuration
				.getProperty("org.sagebionetworks.id.generator.database.username");
	}

	/**
	 * The password used for the ID Generator.
	 * 
	 * @return
	 */
	public String getIdGeneratorDatabasePassword() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.id.generator.database.password");
	}

	public String getIdGeneratorDatabaseDriver() {
		return configuration
				.getProperty("org.sagebionetworks.id.generator.database.driver");
	}

	/**
	 * All of these keys are used to build up a map of JDO configurations passed
	 * to the JDOPersistenceManagerFactory
	 */
	private static String[] MAP_PROPERTY_NAME = new String[] {
			"javax.jdo.PersistenceManagerFactoryClass",
			"datanucleus.NontransactionalRead",
			"datanucleus.NontransactionalWrite",
			"javax.jdo.option.RetainValues", "datanucleus.autoCreateSchema",
			"datanucleus.validateConstraints", "datanucleus.validateTables",
			"datanucleus.transactionIsolation", };

	public Map<String, String> getRepositoryJDOConfigurationMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		for (String name : MAP_PROPERTY_NAME) {
			String value = configuration.getProperty(name);
			if (value == null)
				throw new IllegalArgumentException("Failed to find property: "
						+ name);
			map.put(name, value);
		}
		map.put("javax.jdo.option.ConnectionURL",
				getRepositoryDatabaseConnectionUrl());
		map.put("javax.jdo.option.ConnectionDriverName",
				getRepositoryDatabaseDriver());
		map.put("javax.jdo.option.ConnectionUserName",
				getRepositoryDatabaseUsername());
		map.put("javax.jdo.option.ConnectionPassword",
				getRepositoryDatabasePassword());
		// See PLFM-852
		map.put("datanucleus.cache.level2.type", "none");
		map.put("datanucleus.cache.query.type", "none");
		map.put("datanucleus.cache.collections", "false");
		map.put("datanucleus.cache.level1.type", "weak");
		return map;
	}

	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseDriver() {
		return configuration
				.getProperty("org.sagebionetworks.id.generator.database.driver");
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
		return configuration
				.getProperty("org.sagebionetworks.repository.database.connection.url");
	}

	/**
	 * The repository database username.
	 * 
	 * @return
	 */
	public String getRepositoryDatabaseUsername() {
		return configuration
				.getProperty("org.sagebionetworks.repository.database.username");
	}

	/**
	 * The repository database password.
	 * 
	 * @return
	 */
	public String getRepositoryDatabasePassword() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.repository.database.password");
	}

	/**
	 * Should the connection pool connections be validated?
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolShouldValidate() {
		return configuration
				.getProperty("org.sagebionetworks.pool.connection.validate");
	}

	/**
	 * The SQL used to validate pool connections
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolValidateSql() {
		return configuration
				.getProperty("org.sagebionetworks.pool.connection.validate.sql");
	}

	/**
	 * The minimum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMinNumberConnections() {
		return configuration
				.getProperty("org.sagebionetworks.pool.min.number.connections");
	}

	/**
	 * The maximum number of connections in the pool
	 * 
	 * @return
	 */
	public String getDatabaseConnectionPoolMaxNumberConnections() {
		return configuration
				.getProperty("org.sagebionetworks.pool.max.number.connections");
	}

	public static int getHttpClientMaxConnsPerRoute() {
		return configuration.getHttpClientMaxConnsPerRoute();
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestUserOneName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.username.one");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestUserOneEmail() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.email.one");
	}

	/**
	 * @return The password of a user for integration tests
	 */
	public static String getIntegrationTestUserOnePassword() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.password.one");
	}

	/**
	 * @return The name of a second user for integration tests
	 */
	public static String getIntegrationTestUserTwoName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.username.two");
	}

	/**
	 * @return The password of a second user for integration tests
	 */
	public static String getIntegrationTestUserTwoPassword() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.password.two");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestUserThreeName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.username.three");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestUserThreeEmail() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.email.three");
	}

	/**
	 * @return The password of a user for integration tests
	 */
	public static String getIntegrationTestUserThreePassword() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.password.three");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestUserThreeDisplayName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.displayname.three");
	}

	/**
	 * @return The name of a second user for integration tests
	 */
	public static String getIntegrationTestUserAdminName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.username.admin");
	}

	/**
	 * @return The password of a second user for integration tests
	 */
	public static String getIntegrationTestUserAdminPassword() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.password.admin");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestRejectTermsOfUseName() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.username.rejecttermsofuse");
	}

	/**
	 * @return The name of a user for integration tests
	 */
	public static String getIntegrationTestRejectTermsOfUseEmail() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.email.rejecttermsofuse");
	}

	/**
	 * @return The password of a user for integration tests
	 */
	public static String getIntegrationTestRejectTermsOfUsePassword() {
		return configuration
				.getProperty("org.sagebionetworks.integration.test.password.rejecttermsofuse");
	}

	/**
	 * @return whether controller logging is enabled or not.
	 */
	public boolean getControllerLoggingEnabled() {
		return Boolean.parseBoolean(configuration
					.getProperty("org.sagebionetworks.usage.metrics.logging.enabled"));
	}

	/**
	 * @return whether log sweeping should be enabled for this stack
	 */
	public static boolean getLogSweepingEnabled() {
		return Boolean.parseBoolean(configuration
					.getProperty("org.sagebionetworks.logging.sweeper.enabled"));
	}

	/**
	 * @return whether the log files should be deleted after they are successfully
	 * pushed to S3
	 */
	public static boolean getDeleteAfterSweepingEnabled() {
		return Boolean.parseBoolean(configuration
					.getProperty("org.sagebionetworks.logging.sweeper.delete.enabled"));
	}

	/**
	 * @return the name of the S3 Bucket where logs are stored
	 * each stack (dev, staging, prod) and each instance of each stack
	 * will have it's own subfolder in this bucket
	 */
	public static String getS3LogBucket() {
		return configuration.getProperty("org.sagebionetworks.logging.sweeper.bucket");
	}

	/**
	 * @return whether the cloudWatch profiler should be on or off boolean. True
	 *         means on, false means off.
	 */
	public boolean getCloudWatchOnOff() {
		// Boolean toReturn =
		// Boolean.getBoolean(getProperty("org.sagebionetworks.cloud.watch.report.enabled"));
		String answer = configuration
				.getProperty("org.sagebionetworks.cloud.watch.report.enabled");
		boolean theValue = Boolean.parseBoolean(answer);
		return theValue;
	}

	/**
	 * @return the time in milliseconds for the cloudWatch profiler's trigger. I
	 *         till trigger and send metrics to cloudWatch ever xxx
	 *         milliseconds.
	 */
	public long getCloudWatchTriggerTime() {
		return Long.valueOf(configuration
				.getProperty("org.sagebionetworks.cloud.watch.trigger"));
	}

	/**
	 * The maximum number of threads to be used for backup/restore
	 * 
	 * @return
	 */
	public int getBackupRestoreThreadPoolMaximum() {
		return Integer
				.valueOf(configuration
						.getProperty("org.sagebionetworks.backup.restore.thread.pool.maximum"));
	}

	/**
	 * The maximum bytes allowed for a single query result.
	 * 
	 * @return
	 */
	public static long getMaximumBytesPerQueryResult() {
		return Long
				.valueOf(configuration
						.getProperty("org.sagebionetworks.maximum.bytes.per.query.result"));
	}

	/**
	 * The maximum number entities returned in a single call
	 * 
	 * @return
	 */
	public static int getMaximumNumberOfEntitiesReturnedPerCall() {
		return Integer
				.valueOf(configuration
						.getProperty("org.sagebionetworks.maximum.number.entities.returned.per.call"));
	}

	
	/**
	 * The maximum number of pixels used for a preview image height and width
	 * 
	 * @return
	 */
	public static int getMaximumPreivewPixels() {
		return Integer
				.valueOf(configuration
						.getProperty("org.sagebionetworks.preview.image.max.pixels"));
	}
	
	/**
	 * The S3 Bucket for backup file. This is shared across stacks to enable
	 * data migration across a stack.
	 * 
	 * @return
	 */
	public static String getSharedS3BackupBucket() {
		return configuration
				.getProperty("org.sagebionetworks.shared.s3.backup.bucket");
	}

	public static String getBCCSignupEnabled() {
		return configuration
				.getProperty("org.sagebionetworks.bcc.signup.enabled");
	}
	
	public static String getBridgeSpreadsheetTitle() {
		return configuration
				.getProperty("org.sagebionetworks.bridge.spreadsheet.title");
	}
	
	/**
	 * 
	 * Returns the email address to which requests for BCC participation are sent.
	 * 
	 * @return
	 */
	public static String getBCCApprovalEmail() {
		return configuration
				.getProperty("org.sagebionetworks.bcc.approvalEmail");
	}
	
	public static String getGoogleAppsOAuthConsumerKey() {
		return configuration
				.getProperty("org.sagebionetworks.bcc.googleapps.oauth.consumer.key");
	}

	public static String getGoogleAppsOAuthConsumerSecret() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.bcc.googleapps.oauth.consumer.secret");
	}

	public static String getGoogleAppsOAuthAccessToken() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.bcc.googleapps.oauth.access.token");
	}

	public static String getGoogleAppsOAuthAccessTokenSecret() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.bcc.googleapps.oauth.access.token.secret");
	}
	
	public static String getPortalLinkedInKey() {
		return configuration
				.getProperty("org.sagebionetworks.portal.api.linkedin.key");
	}
	
	public static String getPortalLinkedInSecret() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.portal.api.linkedin.secret");
	}

	public static String getPortalGetSatisfactionKey() {
		return configuration
				.getProperty("org.sagebionetworks.portal.api.getsatisfaction.key");
	}
	
	public static String getPortalGetSatisfactionSecret() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.portal.api.getsatisfaction.secret");
	}
	
	/**
	 * The AWS domain name is the <stack>+<stackInstance>
	 * @return
	 */
	public String getAWSDomainName(){
		return getStack()+getStackInstance();
	}
	
	public static String getWorkflowExecutionRetentionPeriodInDays(){
		return configuration.getProperty("org.sagebionetworks.swf.workflowExecutionRetentionPeriodInDays");
	}
	
	/**
	 * Get the ip address of this machine.
	 * @return
	 */
	public static InetAddress getIpAddress(){
		if(address == null){
			try {
				address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				throw new IllegalStateException(e);
			}
		}
		return address;
	}

	/**
	 * The maximum number of entities that can be moved into the trash can at one time.
	 */
	public static int getTrashCanMaxTrashable(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.repo.manager.trash.max.trashable"));
	}

	/**
	 * The name of the AWS topic where repository changes messages are published.
	 * @return
	 */
	public String getRepositoryChangeTopicName(){
		return String.format(StackConstants.TOPIC_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where search updates are pushed.
	 * @return
	 */
	public String getSearchUpdateQueueName(){
		return String.format(StackConstants.SEARCH_QUEUE_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where dynamo updates are pushed.
	 */
	public String getDynamoUpdateQueueName() {
		return String.format(StackConstants.DYNAMO_QUEUE_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}
	
	/**
	 * The name of the AWS SQS where rds updates are pushed.
	 * @return
	 */
	public String getRdsUpdateQueueName(){
		return String.format(StackConstants.RDS_QUEUE_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}
	
	
	/**
	 * The name of the AWS SQS where file updates are pushed.
	 * @return
	 */
	public String getFileUpdateQueueName(){
		return String.format(StackConstants.FILE_QUEUE_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}

	/**
	 * This is the size of a single file transfer memory block used as a buffer.
	 * Note: Due to S3 limitations on the minimum size of a single part of a multi-part upload
	 * this value cannot be less 5 MB.  Currently defaults to 5 MB.
	 * 
	 * @return
	 */
	public long getFileTransferBufferSizeBytes(){
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.buffer.bytes"));
	}
	
	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * @return
	 */
	public double getFileTransferMemoryPercentOfMax(){
		return Double.parseDouble(configuration.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.percent.of.max"));
	}
	
	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * @return
	 */
	public double getFilePreivewMemoryPercentOfMax(){
		return Double.parseDouble(configuration.getProperty("org.sagebionetworks.repo.manager.file.preview.memory.percent.of.max"));
	}

	/**
	 * Validate that fileTransferMemoryPercentOfMax + filePreivewMemoryPercentOfMax does not exceed 90%
	 */
	private void validateFileMemoryPercentages() {
		double transferPercent = getFileTransferMemoryPercentOfMax();
		double previewPercent = getFilePreivewMemoryPercentOfMax();
		if(transferPercent + previewPercent > 0.9) throw new IllegalArgumentException("file.transfer.memory.percent.of.max + file.preview.memory.percent.of.max excceds 0.9 (90%)");
	}
	
	/**
	 * This is the maximum memory used by file transfer memory pool.  Currently defaults to 70% of max memory.
	 * @return
	 */
	public long getMaxFileTransferMemoryPoolBytes(){
		// This is a function of the 
		validateFileMemoryPercentages();
		double transferPercent = getFileTransferMemoryPercentOfMax();
		// Get the max
		return (long) (Runtime.getRuntime().maxMemory() * transferPercent);
	}
	
	/**
	 * The maximum memory that can be used for preview generation.
	 * @return
	 */
	public long getMaxFilePreviewMemoryPoolBytes(){
		// This is a function of the 
		validateFileMemoryPercentages();
		double previewPercent = getFilePreivewMemoryPercentOfMax();
		// Get the max
		return (long) (Runtime.getRuntime().maxMemory() * previewPercent);
	}
	
	/**
	 * Should messages be published to the AWS topic?
	 * @return
	 */
	public boolean getShouldMessagesBePublishedToTopic(){
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.repo.manage.shouldMessagesBePublishedToTopic"));
	}

	/**
	 * EZID user name.
	 */
	public static String getEzidUsername() {
		return configuration.getProperty("org.sagebionetworks.ezid.username");
	}

	/**
	 * EZID password.
	 */
	public static String getEzidPassword() {
		return configuration.getDecryptedProperty("org.sagebionetworks.ezid.password");
	}

	/**
	 * EZID REST API URL.
	 */
	public static String getEzidUrl() {
		return configuration.getProperty("org.sagebionetworks.ezid.url");
	}

	/**
	 * EZID DOI prefix.
	 */
	public static String getEzidDoiPrefix() {
		return configuration.getProperty("org.sagebionetworks.ezid.doi.prefix");
	}

	/**
	 * EZID target URL prefix. Example: https://synapse.prod.sagebase.org/
	 */
	public static String getEzidTargetUrlPrefix() {
		return configuration.getProperty("org.sagebionetworks.ezid.doi.target.url.prefix");
	}
	
	/**
	 * The maximum size of a backup batch.
	 * @return
	 */
	public Long getMigrationBackupBatchMax(){
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.repo.manager.migration.backup.batch.max"));
	}
	
	/**
	 * This should match the Database max_allowed_packet value. See PLFM-1900
	 * @return
	 */
	public Integer getMigrationMaxAllowedPacketBytes(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.repo.model.dbo.migration.max.allowed.packet.byte"));
	}
	
	/**
	 * The maxiumn number of worker in the cluster that will process RDS index data
	 * @return
	 */
	public Long getSemaphoreGatedLockTimeoutMS(){
		return Long.parseLong(configuration.getProperty("org.sagebionetworks.semaphore.gated.lock.timeout.ms"));
	}
	
	/**
	 * The maxiumn number of worker in the cluster that will process RDS index data
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersRds(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.rds"));
	}
	
	/**
	 * The maxiumn number of worker in the cluster that will process search index data
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersSearch(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.search"));
	}
	
	/**
	 * The maxiumn number of worker in the cluster that will process file previews
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersFilePreview(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.file.preview"));
	}
	
	/**
	 * The maxiumn number of worker in the cluster that will process Dynamo index data
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersDynamoIndex(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.dynamo.index"));
	}

	/**
	 * The maxiumn number of worker in the cluster that will synchronize Dynamo with RDS
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersDynamoSynchronize(){
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.semaphore.gated.max.runners.dynamo.synchronize"));
	}
}
