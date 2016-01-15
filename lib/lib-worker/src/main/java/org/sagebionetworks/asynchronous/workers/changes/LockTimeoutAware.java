package org.sagebionetworks.asynchronous.workers.changes;

/**
 * 
 * Workers should implement this interfaces if they need to be aware of lock timeouts.
 *
 */
public interface LockTimeoutAware {

	/**
	 * Will be called with the lock timeout seconds.
	 * 
	 * @param lockTimeoutSec
	 */
	public void setTimeoutSeconds(Long lockTimeoutSec);
}
