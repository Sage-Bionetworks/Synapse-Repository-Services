package org.sagebionetworks.workflow;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.sagebionetworks.TemplatedConfigurationImpl;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.HttpClientHelper;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * @author deflaux
 * 
 */
public class WorkflowTemplatedConfigurationImpl extends
		TemplatedConfigurationImpl implements WorkflowTemplatedConfiguration {
	/**
	 * Default socket timeout for connections to Simple Workflow Service
	 */
	public static final int DEFAULT_SWF_SOCKET_TIMEOUT_SECONDS = 60;

	/**
	 * sharable instance
	 */
	protected AmazonSimpleWorkflow swfClient = null;
	/**
	 * sharable instance
	 */
	protected AmazonSNS snsClient = null;
	/**
	 * sharable instance
	 */
	protected HttpClient httpClient = null;
	/**
	 * sharable instance
	 */
	protected Synapse synapseClient = null;

	/**
	 * @param defaultPropertiesFilename
	 * @param templatePropertiesFilename
	 */
	public WorkflowTemplatedConfigurationImpl(String defaultPropertiesFilename,
			String templatePropertiesFilename) {
		super(defaultPropertiesFilename, templatePropertiesFilename);
	}

	@Override
	public String getSwfEndpoint() {
		String endpoint = getProperty("aws.swf.endpoint");
		if (!endpoint.startsWith("https")) {
			throw new IllegalArgumentException(
					"Use the secure (HTTPS) SWF endpoint");
		}
		return endpoint;
	}

	@Override
	public AmazonSimpleWorkflow createSWFClient() {
		return createSWFClient(DEFAULT_SWF_SOCKET_TIMEOUT_SECONDS);
	}

	@Override
	public AmazonSimpleWorkflow createSWFClient(int socketTimeoutSeconds) {
		ClientConfiguration config = new ClientConfiguration()
				.withSocketTimeout(socketTimeoutSeconds * 1000);
		AWSCredentials awsCredentials = new BasicAWSCredentials(getIAMUserId(),
				getIAMUserKey());
		AmazonSimpleWorkflow client = new AmazonSimpleWorkflowClient(
				awsCredentials, config);
		client.setEndpoint(getSwfEndpoint());
		return client;
	}

	@Override
	public AmazonSimpleWorkflow getSWFClient() {
		if (null == swfClient) {
			synchronized (this) {
				if (null == swfClient) {
					swfClient = createSWFClient();
				}
			}
		}
		return swfClient;
	}

	@Override
	public String getSnsEndpoint() {
		String endpoint = getProperty("aws.sns.endpoint");
		if (!endpoint.startsWith("https")) {
			throw new IllegalArgumentException(
					"Use the secure (HTTPS) SNS endpoint");
		}
		return endpoint;
	}

	@Override
	public AmazonSNS createSNSClient() {
		AWSCredentials snsAWSCredentials = new BasicAWSCredentials(
				getIAMUserId(), getIAMUserKey());
		AmazonSNS client = new AmazonSNSClient(snsAWSCredentials);
		client.setEndpoint(getSnsEndpoint());
		return client;
	}

	@Override
	public AmazonSNS getSNSClient() {
		if (null == snsClient) {
			synchronized (this) {
				if (null == snsClient) {
					snsClient = createSNSClient();
				}
			}
		}
		return snsClient;
	}

	@Override
	public String getSynapseUsername() {
		return getProperty("org.sagebionetworks.synapse.username");
	}

	@Override
	public String getSynapsePassword() {
		return getDecryptedProperty("org.sagebionetworks.synapse.password");
	}
	
	@Override
	public String getSynapseSecretKey() {
		return getDecryptedProperty("org.sagebionetworks.synapse.secretkey");
	}

	@Override
	public Synapse createSynapseClient() throws SynapseException {
		Synapse client = new Synapse();
		client.setRepositoryEndpoint(getRepositoryServiceEndpoint());
		client.setAuthEndpoint(getAuthenticationServicePrivateEndpoint());
		client.login(getSynapseUsername(), getSynapsePassword());
		return client;
	}

	@Override
	public Synapse getSynapseClient() throws SynapseException {
		if (null == synapseClient) {
			synchronized (this) {
				if (null == synapseClient) {
					synapseClient = createSynapseClient();
				}
			}
		}
		return synapseClient;
	}

	@Override
	public HttpClient createHttpClient() {
		HttpClient client = HttpClientHelper.createNewClient(true);
		ThreadSafeClientConnManager manager = (ThreadSafeClientConnManager) client
				.getConnectionManager();
		manager.setDefaultMaxPerRoute(getHttpClientMaxConnsPerRoute());
		return client;
	}

	@Override
	public HttpClient getHttpClient() {
		if (null == httpClient) {
			synchronized (this) {
				if (null == httpClient) {
					httpClient = createHttpClient();
				}
			}
		}
		return httpClient;
	}

	@Override
	public String getRScriptPath() {
		return getProperty("org.sagebionetworks.rScript.path");
	}

}
