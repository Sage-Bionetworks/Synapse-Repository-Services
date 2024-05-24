package org.sagebionetworks.workers.util.aws.message;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunnerConfiguration;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunnerImpl;

import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * A message driven worker consists of three layers:
 * <ol>
 * <li>SemaphoreGatedRunner - This gate is used to control the total number of
 * workers of this type that can run across a cluster of worker machines.</li>
 * <li>MessageQueue - Provides idempotent creation, policy generation, and topic
 * subscription for a single AWS SQS queue. Once constructed a MessageQueue
 * provides information about the queue including the queue URL and ARN.</li>
 * <li>PollingMessageReceiver - This message receiver will establish a long
 * polling request for a single message from the configured AWS queue.</li>
 * </ol>
 * 
 */
public class MessageDrivenWorkerStack implements Runnable {

	Runnable runner;

	public MessageDrivenWorkerStack(CountingSemaphore semaphore,
									AmazonSQSClient awsSQSClient,
									MessageDrivenWorkerStackConfiguration config) {
		// create the queue
		MessageQueueConfiguration queueConfig = config
				.getMessageQueueConfiguration();
		MessageQueueImpl messageQueue = new MessageQueueImpl(awsSQSClient, queueConfig);
		// create the message receiver.
		PollingMessageReceiverConfiguration receiverConfiguration = config
				.getPollingMessageReceiverConfiguration();
		receiverConfiguration.setHasQueueUrl(messageQueue);
		PollingMessageReceiverImpl pollingMessageReceiver = new PollingMessageReceiverImpl(
				awsSQSClient, receiverConfiguration);
		// create the semaphore gated runner
		SemaphoreGatedRunnerConfiguration semaphoreGatedRunnerConfiguration = config
				.getSemaphoreGatedRunnerConfiguration();
		//always ensure that heartbeat config is true
		semaphoreGatedRunnerConfiguration.setRunner(pollingMessageReceiver);
		this.runner = new SemaphoreGatedRunnerImpl(semaphore,
				config.getSemaphoreGatedRunnerConfiguration(), config.getGate());
	}

	@Override
	public void run() {
		runner.run();
	}

}
