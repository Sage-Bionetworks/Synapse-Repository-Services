package org.sagebionetworks.repo.model.exception;

/**
 * Exception throw for operations that can be retried
 * 
 * @author Marco Marasca
 */
public class RecoverableException extends SynapseServerException {

	public RecoverableException(String message, Throwable cause) {
		super(message, cause);
	}

	public RecoverableException(String message) {
		super(message);
	}

	public RecoverableException(Throwable cause) {
		super(cause);
	}	

}
