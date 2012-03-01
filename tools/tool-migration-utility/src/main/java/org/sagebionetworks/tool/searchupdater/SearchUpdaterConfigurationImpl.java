package org.sagebionetworks.tool.searchupdater;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.sagebionetworks.TemplatedConfiguration;
import org.sagebionetworks.TemplatedConfigurationImpl;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.Configuration;
import org.sagebionetworks.tool.migration.SynapseConnectionInfo;
import org.sagebionetworks.utils.HttpClientHelper;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Configuration and client factory for the search updater
 * 
 * @author deflaux
 * 
 */
public class SearchUpdaterConfigurationImpl implements Configuration {

	private static final String DEFAULT_PROPERTIES_FILENAME = "/searchUpdater.properties";
	private static final String TEMPLATE_PROPERTIES = "/searchUpdaterTemplate.properties";

	private TemplatedConfiguration configuration = null;

	/**
	 * 
	 */
	public SearchUpdaterConfigurationImpl() {
		configuration = new TemplatedConfigurationImpl(DEFAULT_PROPERTIES_FILENAME,
				TEMPLATE_PROPERTIES);
		configuration.reloadConfiguration();
	}

	/**
	 * @return the Synapse client configured from this configuration
	 * @throws SynapseException 
	 */
	public SynapseAdministration createSynapseClient() throws SynapseException {
		SynapseAdministration synapse = new SynapseAdministration();

		synapse.setRepositoryEndpoint(getRepositoryServiceEndpoint());
		synapse.setAuthEndpoint(getAuthenticationServicePrivateEndpoint());
		synapse.login(getSynapseAdminUsername(), getSynapseAdminPassword());
		return synapse;
	}

	/**
	 * @return the S3 client configured from this configuration
	 */
	public AmazonS3Client createAmazonS3Client() {
		String iamId = configuration.getIAMUserId();
		String iamKey = configuration.getIAMUserKey();
		if (iamId == null)
			throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)
			throw new IllegalArgumentException("IAM key cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		return client;
	}

	/**
	 * @return the CloudSearch client configured from this configuration
	 */
	public CloudSearchClient createCloudSearchClient() {
		HttpClient httpClient = HttpClientHelper.createNewClient(false); // Don't verify certs
		ThreadSafeClientConnManager manager = (ThreadSafeClientConnManager) httpClient
				.getConnectionManager();
		manager.setDefaultMaxPerRoute(configuration
				.getHttpClientMaxConnsPerRoute());

		return new CloudSearchClient(httpClient, getSearchServiceEndpoint(), getDocumentServiceEndpoint());
	}

	/**
	 * @return the name of the stack to which we are connecting
	 */
	public String getStack() {
		return configuration.getStack();
	}
	/**
	 * @return the Synpase admin username to log in as for this workflow, since
	 *         it needs admin permissions
	 */
	public String getSynapseAdminUsername() {
		return configuration
				.getProperty("org.sagebionetworks.synapse.admin.username");
	}

	/**
	 * @return the Synapse password
	 */
	public String getSynapseAdminPassword() {
		return configuration
				.getDecryptedProperty("org.sagebionetworks.synapse.admin.password");
	}

	/**
	 * @return auth service endpoint
	 */
	public String getAuthenticationServicePrivateEndpoint() {
		return configuration.getAuthenticationServicePrivateEndpoint();
	}

	/**
	 * @return repo service endpoint
	 */
	public String getRepositoryServiceEndpoint() {
		return configuration.getRepositoryServiceEndpoint();
	}

	/**
	 * @return the maximum number of HTTP connections this should have out to a single destination
	 */
	public int getHttpClientMaxConnsPerRoute() {
		return configuration.getHttpClientMaxConnsPerRoute();
	}

	/**
	 * @return CloudSearch document service endpoint
	 */
	public String getDocumentServiceEndpoint() {
		return configuration
				.getProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint");
	}

	/**
	 * @return CloudSearch search service endpoint
	 */
	public String getSearchServiceEndpoint() {
		return configuration
				.getProperty("org.sagebionetworks.cloudsearch.searchservice.endpoint");
	}

	/**
	 * This is the bucket for workflow-related files such as configuration or
	 * search document files. Each workflow should store stuff under its own
	 * workflow name prefix so that we can configure permissions not only on the
	 * bucket but also the S3 object prefix.
	 * @return the name of the S3 bucket for workflow
	 */
	public String getS3WorkflowBucket() {
		return configuration
				.getProperty("org.sagebionetworks.s3.bucket.workflow");
	}

	@Override
	public SynapseConnectionInfo getSourceConnectionInfo() {
		return new SynapseConnectionInfo(
				getAuthenticationServicePrivateEndpoint(),
				getRepositoryServiceEndpoint(), getSynapseAdminUsername(),
				getSynapseAdminPassword());
	}
	@Override
	public SynapseConnectionInfo getDestinationConnectionInfo() {
		throw new IllegalArgumentException(
				"a synapse destination is invalid for this use case");
	}

	@Override
	public int getMaximumNumberThreads() {
		return Integer.parseInt(configuration
				.getProperty("org.sagebionetworks.max.threads"));
	}

	@Override
	public int getMaximumBatchSize() {
		return Integer.parseInt(configuration
				.getProperty("org.sagebionetworks.batch.size"));
	}

	@Override
	public long getWorkerTimeoutMs() {
		return Long.parseLong(configuration
				.getProperty("org.sagebionetworks.worker.thread.timout.ms"));
	}
}
