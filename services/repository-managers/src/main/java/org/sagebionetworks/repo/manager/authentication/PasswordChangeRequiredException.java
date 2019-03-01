package org.sagebionetworks.repo.manager.authentication;

public class PasswordChangeRequiredException extends RuntimeException{
	public PasswordChangeRequiredException(String message){
		super(message);
	}
}
