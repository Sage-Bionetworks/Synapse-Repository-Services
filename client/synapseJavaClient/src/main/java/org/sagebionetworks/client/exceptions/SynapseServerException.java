/**
 * 
 */
package org.sagebionetworks.client.exceptions;


import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * 
 * Abstraction for exception from the server-side.
 *
 */
public abstract class SynapseServerException extends SynapseException {

	private static final long serialVersionUID = 1L;
	private ErrorResponseCode errorResponseCode;

	public SynapseServerException() {
		super();
	}
	
	public SynapseServerException(String message) {
		super(message);
	}
	
	public SynapseServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseServerException(Throwable cause) {
		super(cause);
	}

	public ErrorResponseCode getErrorResponseCode() {
		return errorResponseCode;
	}
}
