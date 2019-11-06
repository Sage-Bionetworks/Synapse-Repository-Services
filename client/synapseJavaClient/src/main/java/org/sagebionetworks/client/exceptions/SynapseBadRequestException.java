package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

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

	public SynapseBadRequestException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}
}
