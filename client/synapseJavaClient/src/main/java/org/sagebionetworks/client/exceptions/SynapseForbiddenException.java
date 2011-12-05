package org.sagebionetworks.client.exceptions;


public class SynapseForbiddenException extends SynapseUserException {
	
	private static final long serialVersionUID = 1L;

	public SynapseForbiddenException() {
		super();
	}

	public SynapseForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseForbiddenException(String message) {
		super(message);
	}

	public SynapseForbiddenException(Throwable cause) {
		super(cause);
	}

}
