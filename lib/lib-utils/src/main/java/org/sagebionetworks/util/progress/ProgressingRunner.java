package org.sagebionetworks.util.progress;

/**
 * A runner that will notify its container of progress made.
 * 
 * @param <T>
 *            The parameter type passed to the
 *            {@link ProgressCallback#progressMade(Object)}.
 */
public interface ProgressingRunner {

	/**
	 * The main run() method for this runner.
	 * 
	 * @param progressCallback
	 *            The runner it expected to call
	 *            {@link ProgressCallback#progressMade(Object)} to notify the
	 *            container that progress is being made.
	 */
	public void run(ProgressCallback progressCallback) throws Exception;

}
