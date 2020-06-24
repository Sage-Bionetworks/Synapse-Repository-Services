package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the OAuth request was malformed.
 *
 * This is used instead of {@link IllegalArgumentException} to adhere to OAuth 2/OIDC specifications
 * for error messages.
 * 
 */
public class OAuthBadRequestException extends OAuthException {
	public OAuthBadRequestException(OAuthErrorCode error) {
		super(error);
	}

	public OAuthBadRequestException(OAuthErrorCode error, String errorDescription) {
		super(error, errorDescription);
	}

	public OAuthBadRequestException(OAuthErrorCode error, Throwable rootCause) {
		super(error, rootCause);
	}

	public OAuthBadRequestException(OAuthErrorCode error, String errorDescription, Throwable rootCause) {
		super(error, errorDescription, rootCause);
	}
}
