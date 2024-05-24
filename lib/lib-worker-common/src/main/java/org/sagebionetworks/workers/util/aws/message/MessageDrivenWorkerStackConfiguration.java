package org.sagebionetworks.workers.util.aws.message;

import java.util.List;

import org.sagebionetworks.workers.util.Gate;
import org.sagebionetworks.workers.util.semaphore.SemaphoreGatedRunnerConfiguration;

/**
 * Wrapper for all of the Configuration needed to create a MessageDrivenWorkerStack.
 *
 */
public class MessageDrivenWorkerStackConfiguration {
	
	MessageQueueConfiguration messageQueueConfiguration;
	PollingMessageReceiverConfiguration pollingMessageReceiverConfiguration;
	SemaphoreGatedRunnerConfiguration semaphoreGatedRunnerConfiguration;
	Gate gate;

	public MessageDrivenWorkerStackConfiguration() {
		messageQueueConfiguration = new MessageQueueConfiguration();
		pollingMessageReceiverConfiguration = new PollingMessageReceiverConfiguration();
		semaphoreGatedRunnerConfiguration = new SemaphoreGatedRunnerConfiguration();
	}

	public MessageQueueConfiguration getMessageQueueConfiguration() {
		return messageQueueConfiguration;
	}

	public PollingMessageReceiverConfiguration getPollingMessageReceiverConfiguration() {
		return pollingMessageReceiverConfiguration;
	}

	public SemaphoreGatedRunnerConfiguration getSemaphoreGatedRunnerConfiguration() {
		return semaphoreGatedRunnerConfiguration;
	}
	
	/**
	 * The name of queue.
	 * 
	 * @param queueName
	 */
	public void setQueueName(String queueName) {
		messageQueueConfiguration.setQueueName(queueName);
	}

	/**
	 * The runner that handles a message pulled from the queue.
	 * @param runner
	 */
	public void setRunner(MessageDrivenRunner runner) {
		pollingMessageReceiverConfiguration.setRunner(runner);
	}
	
	/**
	 * The semaphore lock key that must be held in order to run the runner.
	 * @param lockKey
	 */
	public void setSemaphoreLockKey(String lockKey){
		semaphoreGatedRunnerConfiguration.setLockKey(lockKey);
	}
	
	/**
	 * The maximum number of concurrent locks that can be issued for the given
	 * semaphore key. If the runner is expected to be a singleton, then set this
	 * value to one.
	 * 
	 * @param maxLockCount
	 */
	public void setSemaphoreMaxLockCount(int maxLockCount) {
		semaphoreGatedRunnerConfiguration.setMaxLockCount(maxLockCount);
	}
	
	/**
	 * The lock timeout in seconds for both the MessageVisibilityTimeoutSec and SemaphoreLockTimeoutSec.
	 * @param timeoutSec
	 */
	public void setSemaphoreLockAndMessageVisibilityTimeoutSec(Integer timeoutSec){
		semaphoreGatedRunnerConfiguration.setLockTimeoutSec(timeoutSec);
		pollingMessageReceiverConfiguration.setMessageVisibilityTimeoutSec(timeoutSec);
		pollingMessageReceiverConfiguration.setSemaphoreLockTimeoutSec(timeoutSec);
	}
	
	/**
	 * An optional parameter. When set, each run will only occur if the provided {@link Gate#canRun()} returns true.
	 * @return
	 */
	public Gate getGate() {
		return gate;
	}

	/**
	 * An optional parameter. When set, each run will only occur if the provided {@link Gate#canRun()} returns true.
	 * @param gate
	 */
	public void setGate(Gate gate) {
		this.gate = gate;
		pollingMessageReceiverConfiguration.setGate(gate);
	}

}
