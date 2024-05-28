package org.sagebionetworks.workers.util.semaphore;

import org.sagebionetworks.util.progress.ProgressingRunner;
import org.sagebionetworks.workers.util.Gate;

public class SemaphoreGatedWorkerStackConfiguration {

	private SemaphoreGatedRunnerConfiguration semaphoreGatedRunnerConfig;
	private Gate gate;
	
	public SemaphoreGatedWorkerStackConfiguration(){
		semaphoreGatedRunnerConfig = new SemaphoreGatedRunnerConfiguration();
	}
	
	public SemaphoreGatedRunnerConfiguration getSemaphoreGatedRunnerConfig() {
		return semaphoreGatedRunnerConfig;
	}

	/**
	 * The runner that will be run when the semaphore gate acquires a lock.
	 * 
	 * @param runner
	 */
	public void setProgressingRunner(ProgressingRunner runner){
		this.semaphoreGatedRunnerConfig.setRunner(runner);
	}
	
	/**
	 * The semaphore lock key that must be held in order to run the runner.
	 * @param lockKey
	 */
	public void setSemaphoreLockKey(String lockKey){
		semaphoreGatedRunnerConfig.setLockKey(lockKey);
	}
	
	/**
	 * The maximum number of concurrent locks that can be issued for the given
	 * semaphore key. If the runner is expected to be a singleton, then set this
	 * value to one.
	 * 
	 * @param maxLockCount
	 */
	public void setSemaphoreMaxLockCount(int maxLockCount) {
		semaphoreGatedRunnerConfig.setMaxLockCount(maxLockCount);
	}
	
	/**
	 * The lock timeout in seconds for the semaphore lock.
	 * @param timeoutSec
	 */
	public void setSemaphoreLockTimeoutSec(Integer timeoutSec){
		semaphoreGatedRunnerConfig.setLockTimeoutSec(timeoutSec);
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
	}

}
