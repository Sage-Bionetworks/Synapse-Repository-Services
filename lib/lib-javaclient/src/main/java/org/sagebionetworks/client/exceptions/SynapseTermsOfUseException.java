package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseTermsOfUseException extends SynapseUserException {
	
	private static final long serialVersionUID = 1L;
	
	private static final int STATUS_CODE = HttpStatus.SC_FORBIDDEN;

	public SynapseTermsOfUseException() {
		super(STATUS_CODE);
	}

	public SynapseTermsOfUseException(String message, Throwable cause) {
		super(STATUS_CODE, message, cause);
	}

	public SynapseTermsOfUseException(String message) {
		super(STATUS_CODE, message);
	}

	public SynapseTermsOfUseException(Throwable cause) {
		super(STATUS_CODE, cause);
	}

}
