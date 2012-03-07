package org.sagebionetworks.workflow;

/**
 * This class encapsulates only the summary data typically transferred from a
 * ScriptResult object (which is not serializable and therefore cannot be the
 * result of an activity) that we wish to return from the activity, store in the
 * workflow history, and pass to subsequent workflow steps.
 * 
 * @author deflaux
 * 
 */
public class ActivityScriptResult {

	/**
	 * Even though we are sending stdout and stderr from the R script to a log
	 * file, include a portion of that output in the workflow history for
	 * convenience. We only want to dig through the logs if we need to.
	 */
	private static final int MAX_SCRIPT_OUTPUT = 10240;

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
		// Truncate stdout to a more reasonable size so that we fit
		// withing the workflow state constraints
		this.stdout = MAX_SCRIPT_OUTPUT > stdout.length() ? stdout : stdout
				.substring(0, MAX_SCRIPT_OUTPUT);
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
		// Truncate stderr to a more reasonable size so that we fit
		// withing the workflow state constraints
		this.stderr = MAX_SCRIPT_OUTPUT > stderr.length() ? stderr : stderr
				.substring(0, MAX_SCRIPT_OUTPUT);
	}
}
