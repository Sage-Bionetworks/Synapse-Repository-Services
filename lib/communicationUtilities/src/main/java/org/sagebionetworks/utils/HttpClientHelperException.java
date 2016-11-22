/**
 * 
 */
package org.sagebionetworks.utils;

/**
 * @author deflaux
 * 
 */
public class HttpClientHelperException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int httpStatus = -1;
	private String response = null;

	/**
	 * 
	 */
	public HttpClientHelperException() {
	}

	/**
	 * @param message
	 */
	public HttpClientHelperException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param response
	 */
	public HttpClientHelperException(String message, int responseStatusCode, String responseString) {
		super(message);
		this.httpStatus = responseStatusCode;
		this.response = responseString;
	}

	/**
	 * @return The body of the response, if applicable
	 */
	public String getResponse() {
		return response;
	}

	/**
	 * @return the httpStatus
	 */
	public int getHttpStatus() {
		return httpStatus;
	}

}
