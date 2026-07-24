package org.sagebionetworks.docusign;

/**
 * Signals that DocuSign rejected the access token (HTTP 401), indicating that
 * the cached token should be refreshed and the request retried.
 */
class DocuSignUnauthorizedException extends RuntimeException {

	DocuSignUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}
