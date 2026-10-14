package org.sagebionetworks.repo.web;

/**
 * Thrown when a request is submitted after the Access Requirement it answers has moved onto a new
 * version. An Access Requirement states the terms of access, so a new applicant cannot apply under
 * superseded terms. Carries its own error code so that a client can report that the requirement
 * changed rather than that the answers are invalid.
 */
public class AccessRequirementVersionOutOfDateException extends RuntimeException {

	public AccessRequirementVersionOutOfDateException(String message) {
		super(message);
	}

}
