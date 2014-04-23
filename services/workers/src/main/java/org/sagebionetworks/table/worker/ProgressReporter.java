package org.sagebionetworks.table.worker;

/**
 * Abstraction for a progress reporter.
 * 
 * @author John
 * 
 */
public interface ProgressReporter {

	/**
	 * Try to report progress. This method should be safe to call from a tight
	 * loop as progress will only be reported when the underlining
	 * implementation conditions are met. It is the job of the implementation to
	 * prevent overloading the database with updates.
	 * 
	 * @param rowNumber
	 */
	public void tryReportProgress(int rowNumber);
}
