/**
 * 
 */
package org.sagebionetworks.utils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

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
	public HttpClientHelperException(String message, HttpResponse response) {
		super(message);
		this.httpStatus = response.getStatusLine().getStatusCode();
		try {
			this.response = (null != response.getEntity()) ? EntityUtils
					.toString(response.getEntity()) : null;
		} catch (Exception e) {
			// This is okay to swallow because its just a best effort to
			// retrieve more info when we are already in an exception situation
		}
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
