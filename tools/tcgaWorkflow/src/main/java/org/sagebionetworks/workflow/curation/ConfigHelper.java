package org.sagebionetworks.workflow.curation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.client.Synapse;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Configuration Helper to used to create Synapse and AWS service clients.
 */
public class ConfigHelper {

	/**
	 * The system property key with which to pass the filepath to the
	 * credentials property file
	 */
	private static final String CREDENTIALS_PROPERTY_KEY = "org.sagebionetworks.credentialsFile";
	/**
	 * The credentials property key with which to pass the Synapse username
	 */
	private static final String USERNAME_KEY = "synapseUsername";
	/**
	 * The credentials property key with which to pass the Synapse password
	 */
	private static final String PASSWORD_KEY = "synapsePassword";
	/**
	 * The credentials property key with which to pass the AWS access key id
	 */
	private static final String ACCESS_ID_KEY = "AWS_ACCESS_KEY_ID";
	/**
	 * The credentials property key with which to pass the AWS secret key
	 */
	private static final String SECRET_KEY_KEY = "AWS_SECRET_KEY";

	private static final String WORKFLOW_PROPERTIES_FILENAME = "workflow.properties";
	private static final String SWF_ENDPOINT_KEY = "swf.endpoint";
	private static final String SNS_ENDPOINT_KEY = "sns.endpoint";
	private static final String SNS_TOPIC_KEY = "sns.topic";
	private static final String S3_BUCKET_KEY = "s3.bucket";
	private static final String SCRIPT_TIMEOUT_KEY = "max.script.execution.hours.timeout";
	private static final String LOCAL_CACHE_DIR = "local.cache.dir";

	private String snsEndpoint;
	private String swfEndpoint;

	private String snsTopic;
	private String s3Bucket;
	private int maxScriptExecutionHoursTimeout;
	private String localCacheDir;

	private String synapseUsername;
	private String synapsePassword;
	private String awsAccessId;
	private String awsSecretKey;

	private volatile static ConfigHelper theInstance = null;

	private ConfigHelper() {
		/**
		 * Load the workflow properties (these are checked into svn and in the
		 * classpath)
		 */
		URL url = ClassLoader.getSystemResource(WORKFLOW_PROPERTIES_FILENAME);
		if (null == url) {
			throw new Error("unable to find in classpath "
					+ WORKFLOW_PROPERTIES_FILENAME);
		}
		Properties serviceProperties = new Properties();
		try {
			serviceProperties.load(url.openStream());
		} catch (IOException e) {
			throw new Error(e);
		}

		snsEndpoint = serviceProperties.getProperty(SNS_ENDPOINT_KEY);
		swfEndpoint = serviceProperties.getProperty(SWF_ENDPOINT_KEY);
		snsTopic = serviceProperties.getProperty(SNS_TOPIC_KEY);
		s3Bucket = serviceProperties.getProperty(S3_BUCKET_KEY);
		maxScriptExecutionHoursTimeout = Integer.parseInt(serviceProperties
				.getProperty(SCRIPT_TIMEOUT_KEY));
		localCacheDir = serviceProperties.getProperty(LOCAL_CACHE_DIR);

		/**
		 * Load the credentials properties (these are in a file on the local
		 * filesystem and not checked into svn)
		 */
		String credentialsPropertyFilename = System
				.getProperty(CREDENTIALS_PROPERTY_KEY);
		if (null == credentialsPropertyFilename) {
			throw new Error(
					"Path to credentials file is missing, pass it as JVM args -D"
							+ CREDENTIALS_PROPERTY_KEY);
		}
		Properties credentialsProperties = new Properties();
		try {
			credentialsProperties.load(new FileInputStream(new File(
					credentialsPropertyFilename)));
		} catch (FileNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
		synapseUsername = credentialsProperties.getProperty(USERNAME_KEY);
		synapsePassword = credentialsProperties.getProperty(PASSWORD_KEY);
		awsAccessId = credentialsProperties.getProperty(ACCESS_ID_KEY);
		awsSecretKey = credentialsProperties.getProperty(SECRET_KEY_KEY);

		if ((null == synapseUsername) || (null == synapsePassword)
				|| (null == awsAccessId) || (null == awsSecretKey)) {
			throw new Error(
					"Synapse and/or AWS credentials are missing, make sure "
							+ credentialsPropertyFilename + " contains:\n\t"
							+ USERNAME_KEY + "=theSynapseUsername\n\t"
							+ PASSWORD_KEY + "=theSynapsePassword\n\t"
							+ ACCESS_ID_KEY + "=theAccessKey\n\t"
							+ SECRET_KEY_KEY + "=theSecretKey\n");
		}
	}

	/**
	 * Factory method for Configuration Helper
	 * 
	 * @return the configuration singleton
	 */
	public static ConfigHelper createConfig() {
		if (null == theInstance) {
			synchronized (ConfigHelper.class) {
				if (null == theInstance) {
					theInstance = new ConfigHelper();
				}
			}
		}
		return theInstance;
	}

	/**
	 * Create a synchronous Simple Workflow Framework (SWF) Client
	 * 
	 * @return the SWF client
	 */
	public AmazonSimpleWorkflow createSWFClient() {
		ClientConfiguration config = new ClientConfiguration()
				.withSocketTimeout(70 * 1000);
		AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessId,
				awsSecretKey);
		AmazonSimpleWorkflow client = new AmazonSimpleWorkflowClient(
				awsCredentials, config);
		client.setEndpoint(swfEndpoint);
		return client;
	}

	/**
	 * Create a synchronous Simple Storage Service (S3) client
	 * 
	 * @return the S3 Client
	 */
	public AmazonS3 createS3Client() {
		AWSCredentials s3AWSCredentials = new BasicAWSCredentials(awsAccessId,
				awsSecretKey);
		AmazonS3 client = new AmazonS3Client(s3AWSCredentials);
		return client;
	}

	/**
	 * Create a synchronous Simple Notification Service (SNS) client
	 * 
	 * @return the SNS Client
	 */
	public AmazonSNS createSNSClient() {
		AWSCredentials snsAWSCredentials = new BasicAWSCredentials(awsAccessId,
				awsSecretKey);
		AmazonSNS client = new AmazonSNSClient(snsAWSCredentials);
		client.setEndpoint(snsEndpoint);
		return client;
	}

	/**
	 * @return the Synapse client
	 * @throws Exception
	 */
	public Synapse createSynapseClient() throws Exception {
		Synapse synapse = new Synapse();
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServiceEndpoint());
		synapse.login(synapseUsername, synapsePassword);
		return synapse;
	}

	/**
	 * @return the s3Bucket
	 */
	public String getS3Bucket() {
		return s3Bucket;
	}

	/**
	 * @return the snsTopic
	 */
	public String getSnsTopic() {
		return snsTopic;
	}

	/**
	 * @return the maxScriptExecutionHoursTimeout
	 */
	public int getMaxScriptExecutionHoursTimeout() {
		return maxScriptExecutionHoursTimeout;
	}

	/**
	 * @return the localCacheDir
	 */
	public String getLocalCacheDir() {
		return localCacheDir;
	}

	/**
	 * @return the synapseUsername
	 */
	public String getSynapseUsername() {
		return synapseUsername;
	}

	/**
	 * @return the synapsePassword
	 */
	public String getSynapsePassword() {
		return synapsePassword;
	}
}
