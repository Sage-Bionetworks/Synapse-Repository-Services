package org.sagebionetworks.repo.manager;

public class UserCertificationRequiredException extends RuntimeException {
	public UserCertificationRequiredException(String message){
		super(message);
	}
}
