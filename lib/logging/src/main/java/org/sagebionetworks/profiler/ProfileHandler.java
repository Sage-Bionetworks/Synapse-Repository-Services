package org.sagebionetworks.profiler;

public interface ProfileHandler {
	
	/**
	 * Should profile data be captured?
	 * @param args
	 * @return
	 */
	public boolean shouldCaptureProfile(Object[] args);
	
	/**
	 * Called after the frame data has been captured.
	 * @param data
	 */
	public void fireProfile(Frame data);

}
