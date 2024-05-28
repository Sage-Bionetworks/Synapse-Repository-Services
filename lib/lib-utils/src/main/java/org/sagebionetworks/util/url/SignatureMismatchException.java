package org.sagebionetworks.util.url;

/**
 * Thrown when a pre-signed URL is not valid.
 *
 */
public class SignatureMismatchException extends Exception {

	private static final long serialVersionUID = 1L;

	public SignatureMismatchException() {
		super();
	}

	public SignatureMismatchException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SignatureMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public SignatureMismatchException(String message) {
		super(message);
	}

	public SignatureMismatchException(Throwable cause) {
		super(cause);
	}
	

}
