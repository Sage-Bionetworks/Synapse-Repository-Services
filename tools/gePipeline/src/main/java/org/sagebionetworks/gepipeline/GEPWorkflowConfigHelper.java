package org.sagebionetworks.gepipeline;

import java.io.File;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.workflow.WorkflowTemplatedConfiguration;
import org.sagebionetworks.workflow.WorkflowTemplatedConfigurationImpl;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.sns.AmazonSNS;

/**
 * Configuration Helper to used to create Synapse and AWS service clients.
 */
public class GEPWorkflowConfigHelper {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/gepWorkflow.properties";
	private static final String TEMPLATE_PROPERTIES = "/gepWorkflowTemplate.properties";

	private static final Logger log = Logger.getLogger(GEPWorkflowConfigHelper.class
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

		File cacheDir = new File(getLocalCacheDir());
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
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
		return configuration.getHttpClient();
	}

	/**
	 * @return the stack name
	 */
	public static String getStack() {
		return configuration.getStack();
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
		return configuration
				.getProperty("org.sagebionetworks.sns.topic.workflow");
	}

	/**
	 * @return the Synpase username to log in as for this workflow
	 */
	public static String getSynapseUsername() {
		return configuration.getSynapseUsername();
	}
	
	/**
	 * @return the Synapse password 
	 */
	public static String getSynapsePassword() {
		return configuration.getSynapsePassword();
	}
	
	/**
	 * @return the Synapse password 
	 */
	public static String getSynapseSecretKey() {
		return configuration.getSynapseSecretKey();
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
	
	public static String getGEPipelineSourceProjectId() {
		return configuration.getProperty("org.sagebionetworks.gepipeline.sourceProjectId");
	}
	
	
	public static String getGEPipelineTargetProjectId() {
		return configuration.getProperty("org.sagebionetworks.gepipeline.targetProjectId");
	}
	
	public static String getGEPipelineCrawlerScript() {
		return configuration.getProperty("org.sagebionetoworks.gepipeline.crawlerscript");
	}
	
	public static String getGEPipelineWorkflowScript() {
		return configuration.getProperty("org.sagebionetoworks.gepipeline.workflowscript");
	}
	
	public static String getGEPipelineMaxDatasetSize() {
		return configuration.getProperty("org.sagebionetworks.gepipeline.maxdatasetsize");
	}

	public static String getGEPipelineMaxWorkflowInstances() {
		return configuration.getProperty("org.sagebionetworks.gepipeline.maxworkflowinstances");
	}
	
	public static String getGEPipelineNoop() {
		return configuration.getProperty("org.sagebionetworks.gepipeline.noop");
	}
	
	public static int getGEPipelineSmallCapacityGB() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.gepipeline.smallGB"));
	}
	
	public static int getGEPipelineMediumCapacityGB() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.gepipeline.mediumGB"));
	}
	
	public static int getGEPipelineLargeCapacityGB() {
		return Integer.parseInt(configuration.getProperty("org.sagebionetworks.gepipeline.largeGB"));
	}
	
}
