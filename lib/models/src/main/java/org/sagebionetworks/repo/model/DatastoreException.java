/**
 * 
 */
package org.sagebionetworks.repo.model;

/**
 * @author bhoff
 * 
 */
@SuppressWarnings("serial")
public class DatastoreException extends RuntimeException {

	/**
	 * 
	 */
	public DatastoreException() {
	}

	/**
	 * @param message
	 */
	public DatastoreException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public DatastoreException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DatastoreException(String message, Throwable cause) {
		super(message, cause);
	}

}
