package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseException extends Exception {
	private int statusCode;
	
	private static final int DEFAULT_STATUS_CODE = HttpStatus.SC_INTERNAL_SERVER_ERROR;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SynapseException(int httpStatus) {
		super();
		this.statusCode = httpStatus;
	}

	public SynapseException(int httpStatus, String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.statusCode = httpStatus;
	}

	public SynapseException(int httpStatus, String arg0) {
		super(arg0);
		this.statusCode = httpStatus;
	}

	public SynapseException(int httpStatus, Throwable arg0) {
		super(arg0);
		this.statusCode = httpStatus;
	}
	
	public SynapseException() {
		super();
		this.statusCode = DEFAULT_STATUS_CODE;
	}

	public SynapseException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.statusCode = DEFAULT_STATUS_CODE;
	}

	public SynapseException(String arg0) {
		super(arg0);
		this.statusCode = DEFAULT_STATUS_CODE;
	}

	public SynapseException(Throwable arg0) {
		super(arg0);
		this.statusCode = DEFAULT_STATUS_CODE;
	}
	
	public int getStatusCode() {return statusCode;}

}
