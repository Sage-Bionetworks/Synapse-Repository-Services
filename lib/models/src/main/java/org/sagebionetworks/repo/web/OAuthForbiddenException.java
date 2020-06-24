package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the OAuth request was malformed.
 *
 * This is used instead of {@link ForbiddenException} to adhere to OAuth 2/OIDC specifications
 * for error messages.
 * 
 */
public class OAuthForbiddenException extends OAuthException {
	public OAuthForbiddenException(OAuthErrorCode error) {
		super(error);
	}

	public OAuthForbiddenException(OAuthErrorCode error, String errorDescription) {
		super(error, errorDescription);
	}

	public OAuthForbiddenException(OAuthErrorCode error, Throwable rootCause) {
		super(error, rootCause);
	}

	public OAuthForbiddenException(OAuthErrorCode error, String errorDescription, Throwable rootCause) {
		super(error, errorDescription, rootCause);
	}
}
