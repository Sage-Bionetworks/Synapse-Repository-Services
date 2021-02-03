package org.sagebionetworks.repo.model.exception;

/**
 * Generic exception root class for the synapse backend
 * 
 * @author Marco Marasca
 *
 */
public class SynapseServerException extends RuntimeException {

	public SynapseServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseServerException(String message) {
		super(message);
	}

	public SynapseServerException(Throwable cause) {
		super(cause);
	}

}
