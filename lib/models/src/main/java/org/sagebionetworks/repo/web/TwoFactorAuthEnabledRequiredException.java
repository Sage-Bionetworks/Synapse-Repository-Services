package org.sagebionetworks.repo.web;

public class TwoFactorAuthEnabledRequiredException extends RuntimeException {
	
	public static final String ERROR_MESSAGE = "Two factor authentication must be enabled to perform this operation.";

	public TwoFactorAuthEnabledRequiredException() {
		super(ERROR_MESSAGE);
	}

}
