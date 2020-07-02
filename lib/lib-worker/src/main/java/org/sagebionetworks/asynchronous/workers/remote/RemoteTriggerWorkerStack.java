package org.sagebionetworks.asynchronous.workers.remote;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientConfig;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunnerConfiguration;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunnerImpl;

/**
 * Worker stack used to send an SQS message to a configured stack and that acts as a remote worker trigger
 * 
 * @author Marco Marasca
 */
public class RemoteTriggerWorkerStack implements Runnable {
	
	private static final int MAX_LOCK_COUNT = 1;
	private static final long LOCK_TIMEOUT = 60 * 1000;

	private static final int CONNECTION_TIMEOUT = 10 * 1000;
	private static final int SOCKET_TIMEOUT = CONNECTION_TIMEOUT;
	
	private Runnable stack;

	public RemoteTriggerWorkerStack(StackConfiguration stackConfiguration, CountingSemaphore semaphore, RemoteTriggerWorkerStackConfiguration configuration) {
		
		SynapseAdminClient synapseClient = initSynapseClient(stackConfiguration);
		
		RemoteTriggerRunner runner = new RemoteTriggerRunner(configuration, stackConfiguration, synapseClient);

		SemaphoreGatedRunnerConfiguration runnerConfiguration = new SemaphoreGatedRunnerConfiguration(
				runner,
				configuration.getLockKey(),
				LOCK_TIMEOUT,
				MAX_LOCK_COUNT
		);
		
		this.stack = new SemaphoreGatedRunnerImpl(semaphore, runnerConfiguration, configuration.getGate());
	}
	

	private SynapseAdminClient initSynapseClient(StackConfiguration stackConfiguration) {
		SimpleHttpClientConfig config = new SimpleHttpClientConfig();

		config.setConnectTimeoutMs(CONNECTION_TIMEOUT);
		config.setSocketTimeoutMs(SOCKET_TIMEOUT);

		SynapseAdminClient client = new SynapseAdminClientImpl(config);

		client.setRepositoryEndpoint(stackConfiguration.getRemoteTriggerWorkerEndpoint());

		return client;
	}
	
	@Override
	public void run() {
		stack.run();
	}
}
