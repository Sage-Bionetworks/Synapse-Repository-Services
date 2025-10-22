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
	boolean canRun();

}
