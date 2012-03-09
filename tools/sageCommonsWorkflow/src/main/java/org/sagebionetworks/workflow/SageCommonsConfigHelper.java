package org.sagebionetworks.workflow;


import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.HttpClientHelper;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.sns.AmazonSNS;

/**
 * @author deflaux
 *
 */
public class SageCommonsConfigHelper {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/sageCommonsWorkflow.properties";
	private static final String TEMPLATE_PROPERTIES = "/sageCommonsWorkflowTemplate.properties";

	private static final Logger log = Logger.getLogger(SageCommonsConfigHelper.class
			.getName());

	private static WorkflowTemplatedConfiguration configuration = null;

	static {
		configuration = new WorkflowTemplatedConfigurationImpl(
				DEFAULT_PROPERTIES_FILENAME, TEMPLATE_PROPERTIES);
		// Load the stack configuration the first time this class is referenced
		try {
			configuration.reloadConfiguration();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new RuntimeException(t);
		}
	}
	
	/**
	 * @return the wrapped instance of WorkflowTemplatedConfiguration for use by code from lib-workflow
	 */
	public static WorkflowTemplatedConfiguration getConfig() {
		return configuration;
	}

	/**
	 * Get the shared synchronous Simple Workflow Framework (SWF) Client
	 * 
	 * @return the SWF client
	 */
	public static AmazonSimpleWorkflow getSWFClient() {
		return configuration.getSWFClient();
	}

	/**
	 * Get the shared synchronous Simple Notification Service (SNS) client
	 * 
	 * @return the SNS Client
	 */
	public static AmazonSNS getSNSClient() {
		return configuration.getSNSClient();
	}

	/**
	 * Get the shared Synapse client
	 * 
	 * @return the Synapse client
	 * @throws SynapseException
	 */
	public static Synapse getSynapseClient() throws SynapseException {
		return configuration.getSynapseClient();
	}

	/**
	 * Get the shared HttpClient
	 * 
	 * @return the HttpClient
	 */
	public static HttpClient getHttpClient() {
		// TODO PLFM-1041
		HttpClientHelper.setGlobalConnectionTimeout(configuration.getHttpClient(), 100000);
		HttpClientHelper.setGlobalSocketTimeout(configuration.getHttpClient(), 100000);
		return configuration.getHttpClient();
	}

	/**
	 * @return the stack name
	 */
	public static String getStack() {
		return configuration.getStack();
	}

	/**
	 * @return the portal endpoint
	 */
	public static String getPortalEndpoint() {
		return configuration.getPortalEndpoint();
	}
	
	/**
	 * @return whether debug mode is on or off
	 */
	public static boolean debugMode() {
		return configuration.debugMode();
	}
	
	/**
	 * @return the absolute path for the script that this workflow should run
	 */
	public static String getWorkflowScript() {
		return configuration.getProperty("org.sagebionetworks.workflow.script");
	}
}
