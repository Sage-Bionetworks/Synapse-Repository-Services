/**
 * 
 */
package org.sagebionetworks.utils;

import org.apache.commons.httpclient.HttpMethodBase;

/**
 * @author deflaux
 *
 */
public class HttpClientHelperException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HttpMethodBase method = null;
	private int httpStatus = -1;

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
	 * @param method 
	 */
	public HttpClientHelperException(String message, HttpMethodBase method) {
		super(message);
		this.method = method;
	}
	
	/**
	 * @param message
	 * @param httpStatus 
	 */
	public HttpClientHelperException(String message, int httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

	/**
	 * @return the HttpMethodBase
	 */
	public HttpMethodBase getMethod() {
		return method;
	}

	/**
	 * @return the httpStatus
	 */
	public int getHttpStatus() {
		return httpStatus;
	}

}
