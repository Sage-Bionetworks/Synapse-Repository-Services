package org.sagebionetworks.workers.util;

/**
 * A simple gate used to determine if a runner should run.
 *
 */
public interface Gate {
	
	/**
	 * Can the runner run?
	 * 
	 * @return True if the runner can run.
	 */
	public boolean canRun();
	
	/**
	 * Called if an exception is thrown during a run.
	 * 
	 * @param error The exception thrown by the runner.
	 */
	public void runFailed(Exception error);

}
