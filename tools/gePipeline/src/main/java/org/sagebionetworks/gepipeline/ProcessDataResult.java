package org.sagebionetworks.gepipeline;

/**
 * This class encapsulates only the summary data from the processData activity
 * that we wish to return from the activity, store in the workflow history, and
 * pass to subsequent workflow steps.
 * 
 * @author deflaux
 * 
 */
public class ProcessDataResult {
	String result;
	String stdout;
	String stderr;

	/**
	 * @return the result
	 */
	public String getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * @return the stdout
	 */
	public String getStdout() {
		return stdout;
	}

	/**
	 * @param stdout
	 *            the stdout to set
	 */
	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	/**
	 * @return the stderr
	 */
	public String getStderr() {
		return stderr;
	}

	/**
	 * @param stderr
	 *            the stderr to set
	 */
	public void setStderr(String stderr) {
		this.stderr = stderr;
	}
}
