package org.sagebionetworks.repo.web;

public abstract class OAuthException extends RuntimeException {
	private final OAuthErrorCode error;
	private final String errorDescription;

	/**
	 * @param error one of the designated OAuthErrorCodes
	 */
	public OAuthException(OAuthErrorCode error) {
		super(error.name());
		this.error = error;
		this.errorDescription = null;
	}

	/**
	 * @param error one of the designated OAuthErrorCodes
	 * @param errorDescription an optional human readable description
	 */
	public OAuthException(OAuthErrorCode error, String errorDescription) {
		super(error.name() + " " + errorDescription);
		this.error = error;
		this.errorDescription = errorDescription;
	}

	/**
	 * @param error one of the designated OAuthErrorCodes
	 * @param rootCause
	 */
	public OAuthException(OAuthErrorCode error, Throwable rootCause) {
		super(error.name() + " " + rootCause.getMessage(), rootCause);
		this.error = error;
		this.errorDescription = rootCause.getMessage();
	}

	/**
	 * @param error one of the designated OAuthErrorCodes
	 * @param rootCause
	 */
	public OAuthException(OAuthErrorCode error, String errorDescription, Throwable rootCause) {
		super(error.name() + " " + errorDescription, rootCause);
		this.error = error;
		this.errorDescription = errorDescription;
	}

	public OAuthErrorCode getError() {
		return error;
	}

	public String getErrorDescription() {
		return errorDescription;
	}
}
