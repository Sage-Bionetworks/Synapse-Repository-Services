package org.sagebionetworks;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private class StackConfigurationStringPropertyAccessor implements PropertyAccessor<String> {
		private final String name;

		private StackConfigurationStringPropertyAccessor(String name) {
			this.name = name;
		}

		@Override
		public String get() {
			return dynamicConfiguration.getPropertyRepeatedly(this.name);
		}
	}

	private class StackConfigurationLongPropertyAccessor implements PropertyAccessor<Long> {
		private final String name;

		private StackConfigurationLongPropertyAccessor(String name) {
			this.name = name;
		}

		@Override
		public Long get() {
			return Long.parseLong(dynamicConfiguration.getPropertyRepeatedly(this.name));
		}
	}

	private class StackConfigurationIntegerPropertyAccessor implements PropertyAccessor<Integer> {
		private final String name;

		private StackConfigurationIntegerPropertyAccessor(String name) {
			this.name = name;
		}

		@Override
		public Integer get() {
			return Integer.parseInt(dynamicConfiguration.getPropertyRepeatedly(this.name));
		}
	}

	private class StackConfigurationDoublePropertyAccessor implements PropertyAccessor<Double> {
		private final String name;

		private StackConfigurationDoublePropertyAccessor(String name) {
			this.name = name;
		}

		@Override
		public Double get() {
			return Double.parseDouble(dynamicConfiguration.getPropertyRepeatedly(this.name));
		}
	}

	private class StackConfigurationBooleanPropertyAccessor implements PropertyAccessor<Boolean> {
		private final String name;

		private StackConfigurationBooleanPropertyAccessor(String name) {
			this.name = name;
		}

		@Override
		public Boolean get() {
			return Boolean.parseBoolean(dynamicConfiguration.getPropertyRepeatedly(this.name));
		}
	}

	private static final String PROD = "prod";
	private static final String DEV = "dev";
	private static final String HUDSON = "hud";
	
	static final String DEFAULT_PROPERTIES_FILENAME = "/stack.properties";
	static final String TEMPLATE_PROPERTIES = "/template.properties";

	private static final Logger log = LogManager.getLogger(StackConfiguration.class
			.getName());

	private static TemplatedConfiguration configuration = null;
	private static InetAddress address = null;

	private static volatile TemplatedConfiguration dynamicConfiguration = null;

	static {
		init();
	}

	public static void init() {
		configuration = new TemplatedConfigurationImpl(
				DEFAULT_PROPERTIES_FILENAME, TEMPLATE_PROPERTIES);
		// Load the stack configuration the first time this class is referenced
		try {
			configuration.reloadConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}
		dynamicConfiguration = configuration;
	}

	public static void reloadDynamicStackConfiguration() {
		TemplatedConfiguration newConfiguration = new TemplatedConfigurationImpl(DEFAULT_PROPERTIES_FILENAME, TEMPLATE_PROPERTIES);
		newConfiguration.reloadConfiguration();
		dynamicConfiguration = newConfiguration;
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
	 * The stack instance (i.e '1', or '2')
	 * 
	 * @return
	 */
	public static String getStackInstance() {
		return configuration.getStackInstance();
	}

	/**
	 * Is this a production stack?
	 * 
	 * @return
	 */
	public static boolean isProductionStack() {
		return isProduction(getStack());
	}
	
	/**
	 * Does the passed stack string represent prod?
	 * @param stack
	 * @return
	 */
	static boolean isProduction(String stack){
		return PROD.equals(stack);
	}
	/**
	 * Is this a Develop stack?
	 * @return
	 */
	public static boolean isDevelopStack(){
		return isDevelopStack(getStack());
	}
	
	static boolean isDevelopStack(String stack){
		return DEV.equals(stack);
	}
	
	/**
	 * Is this a Hudson stack?
	 * @return
	 */
	public static boolean isHudsonStack(){
		return isHudsonStack(getStack());
	}
	
	static boolean isHudsonStack(String stack){
		return HUDSON.equals(stack);
	}


	/**
	 * In production stacks the instance is numeric. In development and test
	 * stacks the instance is often the developer's name. For production stacks
	 * we need to be able to determine the order the stacks were created. For
	 * example, we staging should always have a higher number than production.
	 * This number is used to provide that order.
	 * 
	 * @return
	 */
	public int getStackInstanceNumber() {
		return getStackInstanceNumber(getStackInstance(), isProductionStack());
	}

	/**
	 * In production stacks the instance is numeric. In development and test
	 * stacks the instance is often the developer's name. For production stacks
	 * we need to be able to determine the order the stacks were created. For
	 * example, we staging should always have a higher number than production.
	 * This number is used to provide that order.
	 * 
	 * @param instance
	 * @param isProd
	 * @return
	 */
	static int getStackInstanceNumber(String instance, boolean isProd) {
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
				// Dev instance strings are short so they should always be in range of an integer.
				// Convert the base 256 character string to a base 10 number
				return new BigInteger(instance.getBytes()).intValue();
			}
		}

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

	public static String getBridgeServiceEndpoint() {
		return configuration.getBridgeServiceEndpoint();
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
	 * 
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
				.getProperty("org.sagebionetworks.repository.databaes.driver");
	}
	
	/**
	 * Driver for the repository service.
	 * 
	 * @return
	 */
	public String getTableDatabaseDriver() {
		return configuration
				.getProperty("org.sagebionetworks.table.databaes.driver");
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
	 * @return The username of the migration admin
	 */
	public static String getMigrationAdminUsername() {
		return configuration.getProperty("org.sagebionetworks.migration.admin.username");
	}

	/**
	 * @return The API key of the migration admin
	 */
	public static String getMigrationAdminAPIKey() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.migration.admin.apikey");
	}

	/**
	 * @return whether controller logging is enabled or not.
	 */
	public boolean getControllerLoggingEnabled() {
		return Boolean
				.parseBoolean(configuration
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
	 * @return whether the log files should be deleted after they are
	 *         successfully pushed to S3
	 */
	public static boolean getDeleteAfterSweepingEnabled() {
		return Boolean
				.parseBoolean(configuration
						.getProperty("org.sagebionetworks.logging.sweeper.delete.enabled"));
	}
	
	public static String getNotificationEmailAddress() {
		return configuration
				.getProperty("org.sagebionetworks.notification.email.address");
	}
	
	public static String getSynapseOpsEmailAddress() {
		return configuration.getProperty("org.sagebionetworks.synapseops.email.address");
	}

	/**
	 * @return the name of the S3 Bucket where logs are stored each stack (dev,
	 *         staging, prod) and each instance of each stack will have it's own
	 *         subfolder in this bucket
	 */
	public static String getS3LogBucket() {
		return configuration
				.getProperty("org.sagebionetworks.logging.sweeper.bucket");
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
	 * @return whether the call performance profiler should be on or off boolean. True means on, false means off.
	 */
	public boolean getCallPerformanceOnOff() {
		return Boolean.parseBoolean(configuration.getProperty("org.sagebionetworks.call.performance.report.enabled"));
	}

	/**
	 * @return the time in milliseconds for the call performance profiler's trigger. It will trigger and log average
	 *         call performance ever xxx milliseconds.
	 */
	public long getCallPerformanceTriggerTime() {
		return Long.valueOf(configuration
				.getProperty("org.sagebionetworks.call.performance.trigger"));
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
	 * The maximum number of pixels used for a preview image width
	 * 
	 * @return
	 */
	public static int getMaximumPreviewWidthPixels() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.preview.image.max.width.pixels"));
	}
	
	/**
	 * The maximum number of pixels used for a preview image height
	 * 
	 * @return
	 */
	public static int getMaximumPreviewHeightPixels() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.preview.image.max.height.pixels"));
	}

	/**
	 * The maximum number of pixels used for an attachment image
	 * 
	 * @return
	 */
	public static int getMaximumAttachmentPreviewPixels() {
		return Integer.valueOf(configuration
				.getProperty("org.sagebionetworks.attachment.preview.image.max.pixels"));
	}

	/**
	 * Is the search feature enabled?
	 * @return
	 */
	public boolean getSearchEnabled(){
		return Boolean.parseBoolean(configuration
				.getProperty("org.sagebionetworks.search.enabled"));
	}
	
	/**
	 * Is the Dynamo feature enabled?
	 * @return
	 */
	public boolean getDynamoEnabled(){
		return Boolean.parseBoolean(configuration
				.getProperty("org.sagebionetworks.dynamo.enabled"));
	}
	
	/**
	 * Is the Table feature enabled?
	 * @return
	 */
	public boolean getTableEnabled(){
		return Boolean.parseBoolean(configuration.getPropertyRepeatedly("org.sagebionetworks.table.enabled"));
	}

	/**
	 * Is the DOI feature enabled?
	 * @return
	 */
	public boolean getDoiEnabled(){
		return Boolean.parseBoolean(configuration
				.getProperty("org.sagebionetworks.doi.enabled"));
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
	 * Returns the email address to which requests for BCC participation are
	 * sent.
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
	 * 
	 * @return
	 */
	public String getAWSDomainName() {
		return getStack() + getStackInstance();
	}

	public static String getWorkflowExecutionRetentionPeriodInDays() {
		return configuration
				.getProperty("org.sagebionetworks.swf.workflowExecutionRetentionPeriodInDays");
	}

	/**
	 * Get the ip address of this machine.
	 * 
	 * @return
	 */
	public static InetAddress getIpAddress() {
		if (address == null) {
			try {
				address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				throw new IllegalStateException(e);
			}
		}
		return address;
	}

	/**
	 * The maximum number of entities that can be moved into the trash can at
	 * one time.
	 */
	public static int getTrashCanMaxTrashable() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.repo.manager.trash.max.trashable"));
	}
	
	/**
	 * Stack and instance: <stack>-<stack_instance>
	 * @return
	 */
	public String getStackAndStackInstancePrefix(){
		return String.format(StackConstants.STACK_AND_INSTANCE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}
	
	/**
	 * The name of the async queue
	 * 
	 * @return
	 */
	public String getAsyncQueueName(String baseName) {
		return String.format(StackConstants.ASYNC_QUEUE_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance(),
				baseName);
	}

	/**
	 * The name of the async queue
	 * 
	 * @return
	 */
	public Map<String,String> getAsyncQueueName() {
		return new DynamicMap<String, String>() {
			@Override
			protected String create(Object key) {
				return getAsyncQueueName(key.toString());
			}
		};
	}

	/**
	 * The name of the async queue
	 * 
	 * @return
	 */
	public String getWorkerQueueName(String baseName) {
		return String.format(StackConstants.WORKER_QUEUE_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance(),
				baseName);
	}

	/**
	 * The name of the async queue
	 * 
	 * @return
	 */
	public Map<String, String> getWorkerQueueName() {
		return new DynamicMap<String, String>() {
			@Override
			protected String create(Object key) {
				return getWorkerQueueName(key.toString());
			}
		};
	}

	/**
	 * The name of the AWS topic where repository changes messages are published.
	 * 
	 * @return
	 */
	public String getRepositoryChangeTopicPrefix() {
		return String.format(StackConstants.TOPIC_NAME_TEMPLATE_PREFIX,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS topic where repository changes messages are published.
	 * 
	 * @return
	 */
	public String getRepositoryModificationTopicName() {
		return String.format(StackConstants.TOPIC_NAME_TEMPLATE_PREFIX, StackConfiguration.getStack(), StackConfiguration.getStackInstance())
				+ "modifications";
	}

	/**
	 * The name of the AWS SQS where search updates are pushed.
	 * 
	 * @return
	 */
	public String getSearchUpdateQueueName() {
		return String.format(StackConstants.SEARCH_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where dynamo updates are pushed.
	 */
	public String getDynamoUpdateQueueName() {
		return String.format(StackConstants.DYNAMO_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where rds updates are pushed.
	 * 
	 * @return
	 */
	public String getRdsUpdateQueueName() {
		return String.format(StackConstants.RDS_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where message (to user) updates are pushed.
	 * 
	 * @return
	 */
	public String getMessageUpdateQueueName() {
		return String.format(StackConstants.MESSAGE_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where file updates are pushed.
	 * 
	 * @return
	 */
	public String getFileUpdateQueueName() {
		return String.format(StackConstants.FILE_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	/**
	 * The name of the AWS SQS where file updates are pushed.
	 * 
	 * @return
	 */
	public String getAnnotationsUpdateQueueName() {
		return String.format(StackConstants.ANNOTATIONS_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}
	
	/**
	 * @return The name of the AWS SQS where ranges of change messages are pushed. 
	 */
	public String getUnsentMessagesQueueName() {
		return String.format(StackConstants.UNSENT_MESSAGES_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}
	
	/**
	 * @return The name of the AWS SQS where user identifier updates are pushed
	 */
	public String getPrincipalHeaderQueueName() {
		return String.format(StackConstants.PRINCIPAL_HEADER_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	public String getTableUpdateQueueName() {
		return String.format(StackConstants.TABLE_CLUSTER_QUEUE_NAME_TEMPLATE,
				StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}

	public String getTableCurrentCacheUpdateQueueName() {
		return String.format(StackConstants.TABLE_CURRENT_CACHE_QUEUE_NAME_TEMPLATE, StackConfiguration.getStack(),
				StackConfiguration.getStackInstance());
	}
	
	
	/**
	 * This is the size of a single file transfer memory block used as a buffer.
	 * Note: Due to S3 limitations on the minimum size of a single part of a
	 * multi-part upload this value cannot be less 5 MB. Currently defaults to 5
	 * MB.
	 * 
	 * @return
	 */
	public long getFileTransferBufferSizeBytes() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.buffer.bytes"));
	}

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFileTransferMemoryPercentOfMax() {
		return Double
				.parseDouble(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.transfer.memory.percent.of.max"));
	}

	/**
	 * The percentage of the maximum memory that can be used for file transfer.
	 * Note: transfer% + preview% cannot exceed 90%
	 * 
	 * @return
	 */
	public double getFilePreivewMemoryPercentOfMax() {
		return Double
				.parseDouble(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.preview.memory.percent.of.max"));
	}

	/**
	 * Validate that fileTransferMemoryPercentOfMax +
	 * filePreivewMemoryPercentOfMax does not exceed 90%
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
		return Boolean
				.parseBoolean(configuration
						.getProperty("org.sagebionetworks.repo.manage.shouldMessagesBePublishedToTopic"));
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
		return configuration
				.getDecryptedProperty("org.sagebionetworks.ezid.password");
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
		return configuration
				.getProperty("org.sagebionetworks.ezid.doi.target.url.prefix");
	}

	/**
	 * The maximum size of a backup batch.
	 * 
	 * @return
	 */
	public Long getMigrationBackupBatchMax() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.repo.manager.migration.backup.batch.max"));
	}

	/**
	 * This should match the Database max_allowed_packet value. See PLFM-1900
	 * 
	 * @return
	 */
	public Integer getMigrationMaxAllowedPacketBytes() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.repo.model.dbo.migration.max.allowed.packet.byte"));
	}

	/**
	 * The maximum number of workers in the cluster that will process RDS index
	 * data
	 * 
	 * @return
	 */
	public Long getSemaphoreGatedLockTimeoutMS() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.lock.timeout.ms"));
	}

	/**
	 * The maximum number of workers in the cluster that will process RDS index
	 * data
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersRds() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.rds"));
	}

	/**
	 * The maximum number of workers in the cluster that will send messages to users
	 */
	public Integer getSemaphoreGatedMaxRunnersMessageToUser() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.message.to.user"));
	}

	/**
	 * The maximum number of workers in the cluster that will process search
	 * index data
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersSearch() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.search"));
	}

	/**
	 * The maximum number of workers in the cluster that will process file
	 * previews
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersFilePreview() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.file.preview"));
	}

	/**
	 * The maximum number of workers in the cluster that will process Dynamo
	 * index data
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersDynamoIndex() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.dynamo.index"));
	}

	/**
	 * The maximum number of workers in the cluster that will synchronize Dynamo
	 * with RDS
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersDynamoSynchronize() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.dynamo.synchronize"));
	}

	
	/**
	 * The maximum number of workers in the cluster that will push UnsentMessageRanges to SQS
	 */
	public Integer getSemaphoreGatedMaxRunnersUnsentMessageQueuer() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.unsent.message.queuer"));
	}
	
	/**
	 * The maximum number of workers in the cluster that will pop UnsentMessageRanges from SQS
	 */
	public Integer getSemaphoreGatedMaxRunnersUnsentMessagePoppers() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.unsent.message.poppers"));
	}
	
	/**
	 * The maximum number of workers in the cluster that will update the PrincipalHeader table from SQS
	 */
	public Integer getSemaphoreGatedMaxRunnersPrincipalHeaderFiller() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.principal.header.filler"));
	}
	
	public Integer getSemaphoreGatedMaxRunnersTableCluster() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.table.cluster"));
	}
	/**
	 * The maximum number of workers in the cluster that will process
	 * Annotations
	 * 
	 * @return
	 */
	public Integer getSemaphoreGatedMaxRunnersAnnotations() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.gated.max.runners.annotations"));
	}
	
	/**
	 * The maximum timeout for an exclusive lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreExclusiveMaxTimeoutMS() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.exclusive.max.timeout.ms"));
	}
	
	/**
	 * The maximum timeout for a shared lock in milliseconds.
	 * 
	 * @return
	 */
	public Integer getSemaphoreSharedMaxTimeoutMS() {
		return Integer
				.parseInt(configuration
						.getProperty("org.sagebionetworks.semaphore.shared.max.timeout.ms"));
	}

	/**
	 * This is the maximum amount of time the upload workers are allowed to take
	 * before timing out.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonTimeoutMS() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.timeout.ms"));
	}

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonMainMaxThreads() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.main.max.threads"));
	}

	/**
	 * The maximum number of threads that can be used for the mutipart upload
	 * daemons copy part sub-task.
	 * 
	 * @return
	 */
	public Long getFileMultipartUploadDaemonCopyPartMaxThreads() {
		return Long
				.parseLong(configuration
						.getProperty("org.sagebionetworks.repo.manager.file.multipart.upload.daemon.copy.part.max.threads"));
	}

	/**
	 * Get credentials for the Jira service account used to create Jira issues
	 * 
	 * @return
	 */
	public static String getJiraUserName() {
		return configuration
				.getProperty("org.sagebionetworks.repo.manager.jira.user.name");
	}

	public static String getJiraUserPassword() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.repo.manager.jira.user.password");
	}

	public static String getBridgeDataMappingEncryptionKey() {
		return "::TODO::";
		// return configuration.getDecryptedProperty("org.sagebionetworks.bridge.data.mapping.encryptionkey");
	}

	public static String getBridgeDataEncryptionKey() {
		return "::TODO::";
		// return configuration.getDecryptedProperty("org.sagebionetworks.bridge.data.encryptionkey");
	}

	/**
	 * Entity path for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityPath() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.path");
	}

	/**
	 * Entity path for the root folder. This is to be bootstrapped.
	 */
	public static String getRootFolderEntityPathStatic() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.path");
	}

	/**
	 * Entity ID for the root folder. This is to be bootstrapped.
	 */
	public String getRootFolderEntityId() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.id");
	}

	/**
	 * Entity ID for the root folder. This is to be bootstrapped.
	 */
	public static String getRootFolderEntityIdStatic() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.root.folder.entity.id");
	}

	/**
	 * Entity path for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityPath() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.path");
	}

	/**
	 * Entity path for the trash folder. This is to be bootstrapped.
	 */
	public static String getTrashFolderEntityPathStatic() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.path");
	}

	/**
	 * Entity ID for the trash folder. This is to be bootstrapped.
	 */
	public String getTrashFolderEntityId() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.id");
	}

	/**
	 * Entity ID for the trash folder. This is to be bootstrapped.
	 */
	public static String getTrashFolderEntityIdStatic() {
		return configuration
				.getProperty("org.sagebionetworks.repo.model.bootstrap.trash.folder.entity.id");
	}
	

	/**
	 * Get the name of the table row bucket.
	 * 
	 * @return
	 */
	public String getTableRowChangeBucketName() {
		return String.format(StackConstants.TABLE_ROW_CHANGE_BUCKET, StackConfiguration.getStack());
	}
	
	/**
	 * Get the name of the participant data bucket.
	 * 
	 * @return
	 */
	public String getParticipantDataBucketName() {
		return String.format(StackConstants.PARTICIPANT_DATA_BUCKET, StackConfiguration.getStack());
	}

	/**
	 * Get the max bytes per HTTP request for a table.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerRequest() {
		return Integer.parseInt(configuration
				.getProperty("org.sagebionetworks.table.max.bytes.per.request"));
	}
	/**
	 * The maximum number of rows in a single table change set file.
	 * 
	 * @return
	 */
	public int getTableMaxBytesPerChangeSet() {
		return Integer.parseInt(configuration
				.getProperty("org.sagebionetworks.table.max.bytes.per.change.set"));
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
	 * The maximum amount of time in MS that the table worker can hold the semaphore lock on the table.
	 * 
	 * @return
	 */
	public long getTableWorkerTimeoutMS() {
		return Long.parseLong(configuration
				.getProperty("org.sagebionetworks.table.worker.timeout.ms"));
	}
	
	/**
	 * The maxiumn amount of time in MS that a table reader can hold a read lock on a table.
	 * @return
	 */
	public long getTableReadTimeoutMS() {
		return Long.parseLong(configuration
				.getProperty("org.sagebionetworks.table.read.timeout.ms"));
	}

	public PropertyAccessor<Integer> getMaxConcurrentRepoConnections() {
		return new StackConfigurationIntegerPropertyAccessor("org.sagebionetworks.max.concurrent.repo.connections");
	}

	/**
	 * The amount of time (MS) the ChangeSentMessageSynchWorker sleeps between pages.
	 * @return
	 */
	public PropertyAccessor<Long> getChangeSynchWorkerSleepTimeMS() {
		return new StackConfigurationLongPropertyAccessor("org.sagebionetworks.worker.change.synch.sleep.ms");
	}
	
	/**
	 * The minium page size used by ChangeSentMessageSynchWorker.
	 * @return
	 */
	public PropertyAccessor<Integer> getChangeSynchWorkerMinPageSize() {
		return new StackConfigurationIntegerPropertyAccessor("org.sagebionetworks.worker.change.synch.min.page.size");
	}
	
	/**
	 * Get the name of the audit record bucket.
	 * 
	 * @return
	 */
	public String getAuditRecordBucketName() {
		return String.format(StackConstants.ACCESS_RECORD_BUCKET, StackConfiguration.getStack());
	}
	
	/**
	 * Get the name of the stack log bucket.
	 * 
	 * @return
	 */
	public String getLogBucketName() {
		return String.format(StackConstants.STACK_LOG_BUCKET, StackConfiguration.getStack());
	}
	
	/**
	 * @return for dev stacks, this controls whether emails are delivered or sent to a file (the default)
	 */
	public static boolean getDeliverEmail() {
		String emailDeliveredString = null;
		try {
			emailDeliveredString = configuration.getProperty("org.sagebionetworks.email.delivered");
		} catch (NullPointerException e) {
			emailDeliveredString = null;
		}
		if (emailDeliveredString==null || emailDeliveredString.length()==0) return false;
		return Boolean.parseBoolean(emailDeliveredString);
	}
	
	/**
	 * 
	 * @return if missing or false then certified user restrictions are in effect.  Setting to true disables.
	 */
	public PropertyAccessor<Boolean> getDisableCertifiedUser() {
		return new StackConfigurationBooleanPropertyAccessor("org.sagebionetworks.disable.certified.user");
	}




	private static StackConfiguration singleton = new StackConfiguration();
	/**
	 * Singleton instance of the stack configuration object.
	 * This can be used for static access to non-static methods.
	 * @return
	 */
	public static StackConfiguration singleton(){
		return singleton;
	}
}
