package org.sagebionetworks.profiler;

public interface ProfileHandler {
	
	/**
	 * Should profile data be captured?
	 * @return
	 */
	public boolean shouldCaptureProfile();
	
	/**
	 * Called after the frame data has been captured.
	 * @param data
	 */
	public void fireProfile(Frame data);

}
