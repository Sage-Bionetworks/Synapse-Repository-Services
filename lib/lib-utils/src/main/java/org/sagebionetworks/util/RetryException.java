package org.sagebionetworks.util;

/**
 * Exception type to indicate that the request should be retried.
 * @author jayhodgson
 *
 */
public class RetryException extends Exception {

	public RetryException() {
	}

	public RetryException(String arg0) {
		super(arg0);
	}

	public RetryException(Throwable arg0) {
		super(arg0);
	}

	public RetryException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
