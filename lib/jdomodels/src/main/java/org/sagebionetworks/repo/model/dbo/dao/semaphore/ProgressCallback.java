package org.sagebionetworks.repo.model.dbo.dao.semaphore;
/**
 * Abstraction passed to a runner to be used for notifying the caller that progress is still being made.
 * 
 * @author jmhill
 *
 */
public interface ProgressCallback {
	
	/**
	 * As progress is made, call this method to notify the caller that progress is still being made.
	 * The caller can then extend the timeout of the runner.
	 */
	public void progressMade();

}
