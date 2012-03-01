package org.sagebionetworks.workflow.curation;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.workflow.WorkflowTemplatedConfiguration;
import org.sagebionetworks.workflow.WorkflowTemplatedConfigurationImpl;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.sns.AmazonSNS;

/**
 * Configuration Helper to used to manage configuration specific to this
 * workflow and access general workflow configuration and clients.
 * 
 * @author deflaux
 */
public class TcgaWorkflowConfigHelper {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/tcgaWorkflow.properties";
	private static final String TEMPLATE_PROPERTIES = "/tcgaWorkflowTemplate.properties";
	private static final String TCGA_ABBREVIATION_PREFIX = "abbrev_";

	private static final Logger log = Logger.getLogger(TcgaWorkflowConfigHelper.class
			.getName());

	private static WorkflowTemplatedConfiguration configuration = null;
	private static Map<String, String> ABBREV2NAME = null;

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

		Map<String, String> abbrev2name = new HashMap<String, String>();
		for (String key : configuration.getAllPropertyNames()) {
			if (key.startsWith(TCGA_ABBREVIATION_PREFIX)) {
				String value = configuration.getProperty(key);
				abbrev2name.put(key
						.substring(TCGA_ABBREVIATION_PREFIX.length()), value);
			}
		}
		ABBREV2NAME = Collections.unmodifiableMap(abbrev2name);
	}

	public static String getTCGADatasetName(String abbreviatedTCGADatasetName) {
		return ABBREV2NAME.get(abbreviatedTCGADatasetName);
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

}
