package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the OAuth request was malformed.
 *
 * This is used instead of {@link org.sagebionetworks.repo.model.UnauthenticatedException} to adhere to OAuth 2/OIDC specifications
 * for error messages.
 * 
 */
public class OAuthUnauthenticatedException extends OAuthException {
	public OAuthUnauthenticatedException(OAuthErrorCode error) {
		super(error);
	}

	public OAuthUnauthenticatedException(OAuthErrorCode error, String errorDescription) {
		super(error, errorDescription);
	}

	public OAuthUnauthenticatedException(OAuthErrorCode error, Throwable rootCause) {
		super(error, rootCause);
	}

	public OAuthUnauthenticatedException(OAuthErrorCode error, String errorDescription, Throwable rootCause) {
		super(error, errorDescription, rootCause);
	}
}
