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
	
	private HttpMethodBase method = null;

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
	 * @param cause
	 */
	public HttpClientHelperException(String message, HttpMethodBase method) {
		super(message);
		this.method = method;
	}
	
	public HttpMethodBase getMethod() {
		return method;
	}

}
