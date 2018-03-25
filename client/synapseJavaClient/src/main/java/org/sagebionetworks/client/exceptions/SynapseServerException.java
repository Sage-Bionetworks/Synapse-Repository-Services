/**
 * 
 */
package org.sagebionetworks.client.exceptions;


/**
 * 
 * Abstraction for exception from the server-side.
 *
 */
public abstract class SynapseServerException extends SynapseException {

	private static final long serialVersionUID = 1L;

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

}
