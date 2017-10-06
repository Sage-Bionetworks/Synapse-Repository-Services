package org.sagebionetworks.worker.job.tracking;

/**
 * Abstraction for a simple job tracker.
 *
 */
public interface JobTracker {

	/**
	 * To track a job, call this method when the job starts.
	 * @param jobName
	 */
	public void jobStarted(String jobName);
	
	/**
	 * To track a job, call this method when the job ends.
	 * @param jobName
	 */
	public void jobEnded(String jobName);

	/**
	 * Consume all of the currently tracked data.
	 * @return
	 */
	public TrackedData consumeTrackedData();
}
