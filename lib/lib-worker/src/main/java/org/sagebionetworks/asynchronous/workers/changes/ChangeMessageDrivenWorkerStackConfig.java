package org.sagebionetworks.asynchronous.workers.changes;

import java.util.List;

import org.sagebionetworks.workers.util.Gate;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenWorkerStackConfiguration;

public class ChangeMessageDrivenWorkerStackConfig {

	ChangeMessageRunner runner;
	MessageDrivenWorkerStackConfiguration config = new MessageDrivenWorkerStackConfiguration();
	
	/**
	 * The name of queue.
	 * 
	 * @param queueName
	 */
	public void setQueueName(String queueName) {
		config.setQueueName(queueName);
	}
	
	/**
	 * The runner that handles a message pulled from the queue.
	 * @param runner
	 */
	public void setRunner(ChangeMessageRunner runner) {
		this.runner = runner;
	}
	
	/**
	 * The semaphore lock key that must be held in order to run the runner.
	 * @param lockKey
	 */
	public void setSemaphoreLockKey(String lockKey){
		config.setSemaphoreLockKey(lockKey);
	}
	
	/**
	 * The maximum number of concurrent locks that can be issued for the given
	 * semaphore key. If the runner is expected to be a singleton, then set this
	 * value to one.
	 * 
	 * @param maxLockCount
	 */
	public void setSemaphoreMaxLockCount(int maxLockCount) {
		config.setSemaphoreMaxLockCount(maxLockCount);
	}
	
	/**
	 * The lock timeout in seconds for both the MessageVisibilityTimeoutSec and SemaphoreLockTimeoutSec.
	 * @param timeoutSec
	 */
	public void setSemaphoreLockAndMessageVisibilityTimeoutSec(Integer timeoutSec){
		config.setSemaphoreLockAndMessageVisibilityTimeoutSec(timeoutSec);
	}
	
	/**
	 * An optional parameter. When set, each run will only occur if the provided {@link Gate#canRun()} returns true.
	 * @param gate
	 */
	public void setGate(Gate gate) {
		config.setGate(gate);
	}
	
	/**
	 * An optional parameter used to subscribe the queue to receive messages
	 * from each topic named in the list.
	 * 
	 * @param topicNamesToSubscribe
	 */
	public void setTopicNamesToSubscribe(List<String> topicNamesToSubscribe){
		config.setTopicNamesToSubscribe(topicNamesToSubscribe);
	}
	
	/**
	 * Optional parameter used to configure this queue to use setup a dead
	 * letter queue for failed messages.
	 * 
	 * If this is set, then {@link #setDeadLetterMaxFailureCount(Integer)} must also be
	 * set.
	 * 
	 * @param deadLetterQueueName
	 *            The name of the dead letter queue where failed messages should
	 *            be pushed when the max failure count is exceeded.
	 */
	public void setDeadLetterQueueName(String deadLetterQueueName) {
		config.setDeadLetterQueueName(deadLetterQueueName);
	}
	
	/**
	 * An optional parameter used to configure this queue to forward failed
	 * messages to a dead letter queue.
	 * 
	 * If this is set then the {@link #setDeadLetterQueueName(Integer)} must
	 * also be set.
	 * 
	 * @param maxFailureCount
	 *            The maximum number of times a message should be retried before
	 *            before being pushed to the dead letter queue.
	 */
	public void setDeadLetterMaxFailureCount(Integer maxFailureCount) {
		config.setDeadLetterMaxFailureCount(maxFailureCount);
	}

	public ChangeMessageRunner getRunner() {
		return runner;
	}

	public MessageDrivenWorkerStackConfiguration getConfig() {
		return config;
	}
	
	/**
	 * When set to true a heartbeat progress event will automatically be generated
	 * as long as  the runner is running.
	 * Defaults to false (no heartbeat).
	 * @param useProgressHeartbeat
	 */
	public void setUseProgressHeartbeat(boolean useProgressHeartbeat) {
		config.setUseProgressHeartbeat(useProgressHeartbeat);
	}
}
