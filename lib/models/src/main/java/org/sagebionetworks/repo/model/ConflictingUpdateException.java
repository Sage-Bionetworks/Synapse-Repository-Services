package org.sagebionetworks.repo.model;


/**
 * Application exception indicating that a resource was more recently updated
 * than the version referenced in the current update request
 * <p>
 * 
 * @author deflaux
 */
public class ConflictingUpdateException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor
	 */
	public ConflictingUpdateException() {
		super(
				"The resource you are attempting to edit has changed since you last fetched the object");
	}

	/**
	 * @param message
	 */
	public ConflictingUpdateException(String message) {
		super(message);
	}

}
