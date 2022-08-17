package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Unknown Synapse Server Exception
 *
 */
public class UnknownSynapseServerException extends SynapseServerException {
	/**
	 * The http response code associated with a server-side exception
	 */
	private int statusCode;
	
	private static final long serialVersionUID = 1L;


	/**
	 * The http response code associated with a server-side exception
	 * @return
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * 
	 */
	public UnknownSynapseServerException(int httpStatus) {
		this(httpStatus, null, null);
	}

	/**
	 * @param message
	 */
	public UnknownSynapseServerException(int httpStatus, String message) {
		this(httpStatus, message, null);
	}

	/**
	 * @param arg0
	 */
	public UnknownSynapseServerException(int httpStatus, Throwable cause) {
		this(httpStatus, null, cause);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public UnknownSynapseServerException(int httpStatus, String message, Throwable cause) {
		this(httpStatus, message, cause, null);
	}

	public UnknownSynapseServerException(int httpStatus, String message, Throwable cause, ErrorResponseCode errorResponseCode) {
		super(createMessage(httpStatus, message), cause, errorResponseCode);
		this.statusCode = httpStatus;
	}

	/**
	 * Create a message from the status code and message.
	 * @param httpStatus
	 * @param message If null then only the status code will be written to the message.
	 * @return
	 */
	private static String createMessage(int httpStatus, String message) {
		StringBuilder builder = new StringBuilder("Status Code: ");
		builder.append(httpStatus);
		if(message != null) {
			builder.append(" message: ");
			builder.append(message);
		}
		return builder.toString();
	}

}
