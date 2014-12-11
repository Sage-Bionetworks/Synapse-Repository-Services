package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseTermsOfUseException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;
	
	private static final int FORBIDDEN_STATUS_CODE = HttpStatus.SC_FORBIDDEN;

	public SynapseTermsOfUseException() {
		super(FORBIDDEN_STATUS_CODE);
	}

	public SynapseTermsOfUseException(String message, Throwable cause) {
		super(FORBIDDEN_STATUS_CODE, message, cause);
	}

	public SynapseTermsOfUseException(String message) {
		super(FORBIDDEN_STATUS_CODE, message);
	}

	public SynapseTermsOfUseException(Throwable cause) {
		super(FORBIDDEN_STATUS_CODE, cause);
	}

}
