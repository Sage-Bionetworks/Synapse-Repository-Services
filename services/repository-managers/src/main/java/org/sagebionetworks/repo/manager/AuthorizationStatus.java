package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UnauthorizedException;

/**
 * Holds the result of an authorization check.
 * If 'authorized' is false then 'reason' gives the user-presentable reason why
 * 
 * @author brucehoff
 *
 */
public class AuthorizationStatus {
	private boolean authorized;
	private RuntimeException exceptionToThrow;
	
	public AuthorizationStatus(boolean authorized, String reason) {
		this(authorized, new UnauthorizedException(reason));
	}
	
	public AuthorizationStatus(boolean authorized, RuntimeException exceptionToThrow) {
		this.authorized = authorized;
		this.exceptionToThrow = exceptionToThrow;
	}
	
	public boolean getAuthorized() {
		return authorized;
	}
	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

	public RuntimeException getExceptionToThrow() {
		return exceptionToThrow;
	}

	public void setExceptionToThrow(RuntimeException exceptionToThrow) {
		this.exceptionToThrow = exceptionToThrow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (authorized ? 1231 : 1237);
		result = prime
				* result
				+ ((exceptionToThrow == null) ? 0 : exceptionToThrow.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthorizationStatus other = (AuthorizationStatus) obj;
		if (authorized != other.authorized)
			return false;
		if (exceptionToThrow == null) {
			if (other.exceptionToThrow != null)
				return false;
		} else if (!exceptionToThrow.equals(other.exceptionToThrow))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AuthorizationStatus [authorized=" + authorized
				+ ", exceptionToThrow=" + exceptionToThrow + "]";
	}

}
