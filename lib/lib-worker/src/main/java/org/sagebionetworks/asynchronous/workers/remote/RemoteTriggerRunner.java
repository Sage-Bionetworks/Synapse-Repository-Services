package org.sagebionetworks.asynchronous.workers.remote;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;

/**
 * Runner that sends an SQS message to a queue on a configured stack endpoint
 * 
 * @author Marco Marasca
 *
 */
public class RemoteTriggerRunner implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(RemoteTriggerRunner.class);

	private String queueName;
	private SQSMessageProvider messageProvider;
	private StackConfiguration stackConfiguration;
	private SynapseAdminClient synapseClient;

	public RemoteTriggerRunner(RemoteTriggerWorkerStackConfiguration configuration, StackConfiguration stackConfiguration, SynapseAdminClient synapseClient) {
		this.queueName = configuration.getQueueName();
		this.messageProvider = configuration.getMessageProvider();
		this.stackConfiguration = stackConfiguration;
		this.synapseClient = synapseClient;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		try {
			String messageBody = messageProvider.getMessageBody(progressCallback);
			String key = stackConfiguration.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
			String secret = stackConfiguration.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);

			synapseClient.setBasicAuthorizationCredentials(key, secret);
			// The retry logic is implemented in the client, if the service is not available it will automatically retry the request
			synapseClient.sendSQSMessage(queueName, messageBody);
		} catch (Throwable ex) {
			LOG.error("Error sending message to " + queueName + "@" + stackConfiguration.getRemoteTriggerWorkerEndpoint(), ex);
		} finally {
			synapseClient.removeAuthorizationHeader();
		}
	}

}
