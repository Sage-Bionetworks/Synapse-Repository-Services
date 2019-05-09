package org.sagebionetworks.repo.manager;

import java.util.Objects;

import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Holds the result of an authorization check.
 * If 'authorized' is false then 'message' gives the user-presentable message for denial
 * 
 * @author brucehoff
 *
 */
public class AuthorizationStatus {
	private static final AuthorizationStatus AUTHORIZED_SINGLETON = new AuthorizationStatus(null);

	// if not null, indicates access denied
	private final RuntimeException denialException;


	//do not expose constuctor. use static methods instead
	private AuthorizationStatus(RuntimeException e) {
		this.denialException = e;
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is authorized.
	 * @return AuthorizationStatus that indicates the action action is authorized.
	 */
	public static AuthorizationStatus authorized(){
		return AUTHORIZED_SINGLETON;
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is denied.
	 * Provide an {@link RuntimeException} that will be thrown when {@link #checkAuthorizationOrElseThrow()} is called
	 * @param denialException
	 * @return a new AuthorizationStatus;
	 */
	public static AuthorizationStatus accessDenied(RuntimeException denialException){
		return new AuthorizationStatus(denialException);
	}

	/**
	 * Create a AuthorizationStatus that indicates the action action is denied.
	 * Will use {@link org.sagebionetworks.repo.model.UnauthorizedException} as the underlying denialException that is
	 * thrown when {@link #checkAuthorizationOrElseThrow()} is called
	 * @param message message for the {@link org.sagebionetworks.repo.model.UnauthorizedException}
	 * @return a new AuthorizationStatus;
	 */
	public static AuthorizationStatus accessDenied(String message){
		return accessDenied(new UnauthorizedException(message));
	}

	public void checkAuthorizationOrElseThrow(){
		if(!isAuthorized()){
			throw denialException;
		}
	}
	
	public boolean isAuthorized() {
		return denialException == null;
	}

	public String getMessage() {
		return denialException == null ? null : denialException.getMessage();
	}

	//The equals() here is not the conventional "check all fields members are equal" because it holds
	// a RuntimeException which does not by default @Override equals()

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuthorizationStatus that = (AuthorizationStatus) o;
		return this.isAuthorized() == that.isAuthorized() &&
				this.exceptionType() == that.exceptionType() &&
				Objects.equals(this.getMessage(), that.getMessage());
	}

	@Override
	public int hashCode() {
		return Objects.hash(isAuthorized(), getMessage(), exceptionType());
	}

	private Class exceptionType (){
		if (denialException == null){
			return null;
		}
		return denialException.getClass();
	}

	@Override
	public String toString() {
		return "AuthorizationStatus{" +
				"denialException=" + denialException +
				'}';
	}
}
