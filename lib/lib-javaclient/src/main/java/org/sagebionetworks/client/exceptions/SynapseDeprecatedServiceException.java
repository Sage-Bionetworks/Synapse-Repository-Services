package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseDeprecatedServiceException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int GONE_STATUS_CODE = HttpStatus.SC_GONE;

	public SynapseDeprecatedServiceException() {
		super(GONE_STATUS_CODE);
	}

	public SynapseDeprecatedServiceException(String message, Throwable cause) {
		super(GONE_STATUS_CODE, message, cause);
	}

	public SynapseDeprecatedServiceException(String message) {
		super(GONE_STATUS_CODE, message);
	}

	public SynapseDeprecatedServiceException(Throwable cause) {
		super(GONE_STATUS_CODE, cause);
	}

}
