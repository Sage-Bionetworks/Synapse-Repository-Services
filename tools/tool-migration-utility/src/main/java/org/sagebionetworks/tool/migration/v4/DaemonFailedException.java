package org.sagebionetworks.tool.migration.v4;

/**
 * Thrown when a daemon fails to execute a job.
 * @author John
 *
 */
public class DaemonFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DaemonFailedException() {
		super();
	}

	public DaemonFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DaemonFailedException(String message) {
		super(message);
	}

	public DaemonFailedException(Throwable cause) {
		super(cause);
	}

}
