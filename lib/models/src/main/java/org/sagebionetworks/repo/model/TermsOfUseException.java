package org.sagebionetworks.repo.model;

public class TermsOfUseException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public TermsOfUseException() {
		super(ServiceConstants.TERMS_OF_USE_ERROR_MESSAGE);
	}
}
