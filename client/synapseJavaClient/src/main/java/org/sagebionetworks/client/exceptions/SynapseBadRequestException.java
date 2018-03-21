package org.sagebionetworks.client.exceptions;

public class SynapseBadRequestException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseBadRequestException() {
		super();
	}

	public SynapseBadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseBadRequestException(String message) {
		super(message);
	}

	public SynapseBadRequestException(Throwable cause) {
		super(cause);
	}

}
