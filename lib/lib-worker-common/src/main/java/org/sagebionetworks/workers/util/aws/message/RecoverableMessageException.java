package org.sagebionetworks.workers.util.aws.message;

/**
 * This exception is throw to indicates that a message cannot be processed at
 * this time but it should be possible to process it in the future.
 * 
 */
public class RecoverableMessageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecoverableMessageException() {
	}

	public RecoverableMessageException(String message) {
		super(message);
	}

	public RecoverableMessageException(Throwable cause) {
		super(cause);
	}

	public RecoverableMessageException(String message, Throwable cause) {
		super(message, cause);
	}

	public RecoverableMessageException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
