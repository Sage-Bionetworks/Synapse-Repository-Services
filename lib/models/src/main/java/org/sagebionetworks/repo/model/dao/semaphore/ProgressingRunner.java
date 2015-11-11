package org.sagebionetworks.repo.model.dao.semaphore;

import org.sagebionetworks.common.util.progress.ProgressCallback;

/**
 * For jobs that need to hold a semaphore for a long period of time, the runner is given
 * a callback that can be used to refresh the timeout.
 * 
 * @author jmhill
 *
 */
public interface ProgressingRunner {

	/**
	 * Simiar to run of a runnable.
	 * @param callback
	 * @throws Exception
	 */
	public void run(ProgressCallback<Void> callback) throws Exception;
}
