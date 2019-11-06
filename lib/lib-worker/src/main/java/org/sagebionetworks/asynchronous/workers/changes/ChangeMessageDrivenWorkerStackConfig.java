package org.sagebionetworks.asynchronous.workers.changes;

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

	public ChangeMessageRunner getRunner() {
		return runner;
	}

	public MessageDrivenWorkerStackConfiguration getConfig() {
		return config;
	}
}
