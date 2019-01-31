package org.sagebionetworks.repo.manager.password;

public class InvalidPasswordException extends RuntimeException{
	public InvalidPasswordException(String message){
		super(message);
	}
}
