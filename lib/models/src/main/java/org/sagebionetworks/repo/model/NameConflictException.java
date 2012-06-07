package org.sagebionetworks.repo.model;

/**
 * Thrown when a name already exists.
 * 
 * @author John
 *
 */
public class NameConflictException extends IllegalArgumentException {

	public NameConflictException() {
		super();
	}

	public NameConflictException(String message, Throwable cause) {
		super(message, cause);
	}

	public NameConflictException(String s) {
		super(s);
	}

	public NameConflictException(Throwable cause) {
		super(cause);
	}

}
