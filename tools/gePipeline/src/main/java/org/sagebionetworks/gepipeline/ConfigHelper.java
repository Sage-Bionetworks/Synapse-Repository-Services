package org.sagebionetworks.gepipeline;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sagebionetworks.TemplatedConfiguration;
import org.sagebionetworks.client.Synapse;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Configuration Helper to used to create Synapse and AWS service clients.
 */
public class ConfigHelper {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/workflow.properties";
	private static final String TEMPLATE_PROPERTIES = "/workflowTemplate.properties";
	private static final String GEP_ABBREVIATION_PREFIX = "abbrev_";

	private static final Logger log = Logger.getLogger(ConfigHelper.class
			.getName());

	private static TemplatedConfiguration configuration = null;
	private static Map<String, String> ABBREV2NAME = null;

	static {
		configuration = new TemplatedConfiguration(DEFAULT_PROPERTIES_FILENAME,
				TEMPLATE_PROPERTIES);
		// Load the stack configuration the first time this class is referenced
		try {
			configuration.reloadStackConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}
		
		File cacheDir = new File(getLocalCacheDir());
		if(!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		
		Map<String, String> abbrev2name = new HashMap<String, String>();
		for(String key : configuration.getAllPropertyNames()) {
			if(key.startsWith(GEP_ABBREVIATION_PREFIX)) {
				String value = configuration.getProperty(key);
				abbrev2name.put(key.substring(GEP_ABBREVIATION_PREFIX.length()), value);
			}
		}
		ABBREV2NAME = Collections.unmodifiableMap(abbrev2name);
	}

	public static String getGEPDatasetName(String abbreviatedGEPDatasetName) {
		return ABBREV2NAME.get(abbreviatedGEPDatasetName);
	}
	
	/**
	 * Create a synchronous Simple Workflow Framework (SWF) Client
	 * 
	 * @return the SWF client
	 */
	public static AmazonSimpleWorkflow createSWFClient() {
		ClientConfiguration config = new ClientConfiguration()
				.withSocketTimeout(70 * 1000);
		String iamUserId = configuration.getIAMUserId();
		if (iamUserId==null || iamUserId.length()==0) 
			throw new RuntimeException("No value for AWS_ACCESS_KEY_ID or org.sagebionetworks.stack.iam.id");
		String iamUserKey = configuration.getIAMUserKey();
		if (iamUserKey==null || iamUserKey.length()==0) 
			throw new RuntimeException("No value for AWS_SECRET_KEY or org.sagebionetworks.stack.iam.key");
		AWSCredentials awsCredentials = new BasicAWSCredentials(iamUserId, iamUserKey);
		AmazonSimpleWorkflow client = new AmazonSimpleWorkflowClient(
				awsCredentials, config);
		String swfEndpoint = configuration.getProperty("aws.swf.endpoint");
		if (swfEndpoint==null || swfEndpoint.length()==0) 
			throw new RuntimeException("Missing property: aws.swf.endpoint.");
		client.setEndpoint(swfEndpoint);
		return client;
	}

	/**
	 * Create a synchronous Simple Notification Service (SNS) client
	 * 
	 * @return the SNS Client
	 */
	public static AmazonSNS createSNSClient() {
		AWSCredentials snsAWSCredentials = new BasicAWSCredentials(configuration.getIAMUserId(),
				configuration.getIAMUserKey());
		AmazonSNS client = new AmazonSNSClient(snsAWSCredentials);
		client.setEndpoint(configuration.getProperty("aws.sns.endpoint"));
		return client;
	}

	/**
	 * @return the Synapse client
	 * @throws Exception
	 */
	public static Synapse createSynapseClient() throws Exception {
		Synapse synapse = new Synapse();

		synapse.setRepositoryEndpoint(getRepositoryServiceEndpoint());
		synapse.setAuthEndpoint(getAuthenticationServicePublicEndpoint());
		synapse.login(getSynapseUsername(), 
				getSynapsePassword());
		return synapse;
	}

	/**
	 * @return the Synpase username to log in as for this workflow
	 */
	public static String getSynapseUsername() {
		return configuration.getProperty("org.sagebionetworks.synapse.username");
	}
	
	/**
	 * @return the Synapse password 
	 */
	public static String getSynapsePassword() {
		return configuration.getProperty("org.sagebionetworks.synapse.password");
	}
	
	/**
	 * @return the Synapse password 
	 */
	public static String getSynapseSecretKey() {
		return configuration.getProperty("org.sagebionetworks.synapse.secretkey");
	}
	
	/**
	 * @return the localCacheDir
	 */
	public static String getLocalCacheDir() {
		return configuration.getProperty("org.sagebionetworks.localCacheDir");
	}

	/**
	 * @return the Simple Notification Service topic to use for this workflow
	 */
	public static String getWorkflowSnsTopic() {
		return configuration.getProperty("org.sagebionetworks.sns.topic.workflow");
	}
	
	/**
	 * @return auth service endpoint
	 */
	public static String getAuthenticationServicePrivateEndpoint() {
		return configuration.getAuthenticationServicePrivateEndpoint();
	}
	
	/**
	 * @return auth service endpoint
	 */
	public static String getAuthenticationServicePublicEndpoint() {
		return configuration.getAuthenticationServicePublicEndpoint();
	}
	
	/**
	 * @return repo service endpoint
	 */
	public static String getRepositoryServiceEndpoint() {
		return configuration.getRepositoryServiceEndpoint();
	}

	/**
	 * @return the portal endpoint
	 */
	public static String getPortalEndpoint() {
		return configuration.getPortalEndpoint();
	}
	
	public static String getRScriptPath() {
		return configuration.getRScriptPath();
	}
}
