package org.sagebionetworks.repo.model.exception;

/**
 * This exception can be thrown for write operations that occur while the stack status is set to read-only or down.
 * 
 * @author John
 *
 */
public class ReadOnlyException extends RuntimeException {

	public ReadOnlyException() {
		super();
	}

	public ReadOnlyException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ReadOnlyException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReadOnlyException(String message) {
		super(message);
	}

	public ReadOnlyException(Throwable cause) {
		super(cause);
	}

}
