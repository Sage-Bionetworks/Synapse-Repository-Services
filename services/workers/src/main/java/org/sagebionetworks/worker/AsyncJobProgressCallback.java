package org.sagebionetworks.worker;

/**
 * Callback for {@link AsyncJobRunner}s that update the job progress
 */
public interface AsyncJobProgressCallback {

	/**
	 * Can be invoked by the worker in order to update the job progress with a customized message and/or progress
	 * 
	 * @param message The message that will be set on the job in progress
	 * @param progressCurrent The current progress relative to progressTotal
	 * @param progressTotal The total progress
	 */
	void updateProgress(String message, Long progressCurrent, Long progressTotal);
}
