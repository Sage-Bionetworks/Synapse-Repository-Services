package org.sagebionetworks.client.exceptions;

/**
 * Exception throw for HTTP status code of 403.
 *
 */
public class SynapseTermsOfUseException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseTermsOfUseException() {
		super();
	}

	public SynapseTermsOfUseException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseTermsOfUseException(String message) {
		super(message);
	}

	public SynapseTermsOfUseException(Throwable cause) {
		super(cause);
	}

}
