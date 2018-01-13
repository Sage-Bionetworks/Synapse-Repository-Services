package org.sagebionetworks.search;

public class CloudSearchServerException extends RuntimeException{

	/**
	 *
	 */
	private static final long serialVersionUID = 5582738234605784919L;
	private int statusCode;

	public CloudSearchServerException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
