package org.sagebionetworks.tool.migration.v3;

import java.util.List;

/**
 * Abstraction for a batch worker.
 * @author John
 *
 */
public interface BatchWorker {

	/**
	 * Attempt a batch of work.
	 * @param batch
	 * @throws DaemonFailedException - Should be thrown if the daemon fails.
	 * 
	 */
	public Long attemptBatch(List<Long> batch) throws DaemonFailedException, Exception;
}
