package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the OAuth request was malformed.
 *
 * This is used instead of {@link org.sagebionetworks.repo.model.UnauthorizedException} to adhere to OAuth 2/OIDC specifications
 * for error messages.
 * 
 */
public class OAuthUnauthorizedException extends OAuthException {
	public OAuthUnauthorizedException(OAuthErrorCode error) {
		super(error);
	}

	public OAuthUnauthorizedException(OAuthErrorCode error, String errorDescription) {
		super(error, errorDescription);
	}

	public OAuthUnauthorizedException(OAuthErrorCode error, Throwable rootCause) {
		super(error, rootCause);
	}

	public OAuthUnauthorizedException(OAuthErrorCode error, String errorDescription, Throwable rootCause) {
		super(error, errorDescription, rootCause);
	}
}
