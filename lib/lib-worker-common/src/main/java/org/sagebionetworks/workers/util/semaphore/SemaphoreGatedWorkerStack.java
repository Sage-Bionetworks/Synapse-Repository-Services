package org.sagebionetworks.workers.util.semaphore;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.workers.util.Gate;

/**
 * A semaphore gated worker stack that consists of two layers:
 * <ol>
 * <li>SemaphoreGatedRunner - This gate is used to control the total number of
 * workers of this type that can run across a cluster of worker machines. See {@link SemaphoreGatedRunner}.</li>
 *
 * //TODO: fix documentation
 * <li>Gate - When a gate is provided in the configuration, a  will
 * be as the root of the stack, such that the stack will only run when the
 * {@link Gate#canRun()} returns true.</li>
 * </ol>
 * 
 */
public class SemaphoreGatedWorkerStack implements Runnable {

	private Runnable runner;
	//TODO: remove
	public SemaphoreGatedWorkerStack(CountingSemaphore semaphore,
			SemaphoreGatedWorkerStackConfiguration config) {


		//TODO: pass gate (optional) to semaphoreGatedRunner which then creates a listener that uses the gate.
		//just pass SemaphoreGatedWorkerStackConfiguration  to semaphoreGatedRunner. get rid of this layer.
		this.runner = new SemaphoreGatedRunnerImpl(
				semaphore, config.getSemaphoreGatedRunnerConfig(), config.getGate());
	}

	@Override
	public void run() {
		runner.run();
	}

}
