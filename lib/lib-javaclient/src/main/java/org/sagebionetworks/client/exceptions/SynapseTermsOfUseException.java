package org.sagebionetworks.client.exceptions;


public class SynapseTermsOfUseException extends SynapseUserException {
	
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
