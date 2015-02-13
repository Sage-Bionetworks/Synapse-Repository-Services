/**
 * 
 */
package org.sagebionetworks.repo.model;

/**
 * @author deflaux
 * 
 */
public class InvalidModelException extends RuntimeException {

	/**
     * 
     */
	private static final long serialVersionUID = 1130296414124822224L;

	/**
	 * Default constructor
	 */
	public InvalidModelException() {
		super(
				"The model is missing required field(s) and/or has invalid value(s) for field(s).");
	}

	/**
	 * @param message
	 */
	public InvalidModelException(String message) {
		super(message);
	}

	public InvalidModelException(Throwable t) {
		super(t);
	}

	public InvalidModelException(String message, Throwable t) {
		super(message, t);
	}

}
