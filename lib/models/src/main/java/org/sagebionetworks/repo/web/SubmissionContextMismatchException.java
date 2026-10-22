package org.sagebionetworks.repo.web;

/**
 * Thrown when the submission context carried by a request's data contradicts whether that request
 * is an initial request or a renewal. Carries its own error code so that a client can tell this
 * apart from the answers failing the schema.
 */
public class SubmissionContextMismatchException extends RuntimeException {

	public SubmissionContextMismatchException(String message) {
		super(message);
	}

}
