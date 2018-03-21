package org.sagebionetworks.client.exceptions;

/**
 * Thrown for HTTP status code of 410.
 *
 */
public class SynapseDeprecatedServiceException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseDeprecatedServiceException() {
		super();
	}

	public SynapseDeprecatedServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseDeprecatedServiceException(String message) {
		super(message);
	}

	public SynapseDeprecatedServiceException(Throwable cause) {
		super(cause);
	}

}
