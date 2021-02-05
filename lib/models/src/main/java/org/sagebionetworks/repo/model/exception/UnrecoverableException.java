package org.sagebionetworks.repo.model.exception;

/**
 * Exception used for exceptions that are do not need to be recovered
 * 
 * @author Marco Marasca
 */
public class UnrecoverableException extends SynapseServerException {

	public UnrecoverableException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnrecoverableException(String message) {
		super(message);
	}

	public UnrecoverableException(Throwable cause) {
		super(cause);
	}

}
