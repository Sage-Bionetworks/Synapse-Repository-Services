package org.sagebionetworks.client.exceptions;

/**
 * Exception throw for HTTP status code of 503.
 *
 */
public class SynapseServiceUnavailable extends SynapseServerException {

	private static final long serialVersionUID = 1L;

	public SynapseServiceUnavailable(String message) {
		super(message);
	}

	public SynapseServiceUnavailable(Throwable cause) {
		super(cause);
	}

}
