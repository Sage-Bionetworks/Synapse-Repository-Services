/**
 * 
 */
package org.sagebionetworks.client.exceptions;


import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * 
 * Abstraction for exception from the server-side.
 *
 */
public abstract class SynapseServerException extends SynapseException {

	private static final long serialVersionUID = 1L;
	private ErrorResponseCode errorResponseCode;

	private Integer httpStatusCode;

	public SynapseServerException() {
		super();
	}
	
	public SynapseServerException(String message) {
		super(message);
	}
	
	public SynapseServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseServerException(Throwable cause) {
		super(cause);
	}

	public SynapseServerException(String message, ErrorResponseCode errorResponseCode){
		this(message, null, errorResponseCode, null);
	}

	public SynapseServerException(String message, ErrorResponseCode errorResponseCode, Integer httpStatusCode){
		this(message, null, errorResponseCode, httpStatusCode);
	}

	public SynapseServerException(String message, Throwable cause, ErrorResponseCode errorResponseCode, Integer httpStatusCode){
		super(message, cause, httpStatusCode);
		this.errorResponseCode = errorResponseCode;
		this.httpStatusCode = httpStatusCode;
	}

	public ErrorResponseCode getErrorResponseCode() {
		return errorResponseCode;
	}
}
