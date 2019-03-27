package org.sagebionetworks.repo.manager.authentication;

public class PasswordResetViaEmailRequiredException extends RuntimeException{
	public PasswordResetViaEmailRequiredException(String message){
		super(message);
	}
}
