/**
 * 
 */
package org.sagebionetworks.workflow;

/**
 * @author deflaux
 *
 */
public class UnrecoverableException extends Exception {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "This exception needs human intervention to repair the workflow";

	/**
	 * Default constructor
	 */
	public UnrecoverableException() {
		super(DEFAULT_MESSAGE);
	}

	/**
	 * @param message
	 */
	public UnrecoverableException(String message) {
		super(message);
	}

	/**
	 * @param rootCause
	 */
	public UnrecoverableException(Throwable rootCause) {
		super(DEFAULT_MESSAGE, rootCause);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public UnrecoverableException(String message, Throwable rootCause) {
		super(message, rootCause);
	}	
}
