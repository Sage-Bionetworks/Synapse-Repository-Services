/**
 * 
 */
package org.sagebionetworks.utils;

import org.apache.http.HttpResponse;


/**
 * @author deflaux
 *
 */
public class HttpClientHelperException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HttpResponse response = null;

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
	public HttpClientHelperException(String message, HttpResponse response) {
		super(message);
		this.response = response;
	}

	/**
	 * @return the HttpResponse
	 */
	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * @return the httpStatus
	 */
	public int getHttpStatus() {
		return response.getStatusLine().getStatusCode();
	}

}
