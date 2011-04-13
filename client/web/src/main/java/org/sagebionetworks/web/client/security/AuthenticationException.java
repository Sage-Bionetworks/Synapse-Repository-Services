package org.sagebionetworks.web.client.security;

/**
 * Thrown when authentication fails.
 * 
 * @author jmhill
 *
 */
public class AuthenticationException extends Exception {

	public AuthenticationException() {
		super();
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}

}
