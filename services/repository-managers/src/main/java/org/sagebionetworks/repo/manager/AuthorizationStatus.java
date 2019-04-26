package org.sagebionetworks.repo.manager;

import java.util.Objects;

/**
 * Holds the result of an authorization check.
 * If 'authorized' is false then 'message' gives the user-presentable message for denial
 * 
 * @author brucehoff
 *
 */
public class AuthorizationStatus {
	final private boolean authorized;
	final private String message;
	final private AuthorizationStatusDenialReason denialReason;
	
	
	public AuthorizationStatus(boolean authorized, String message) {
		this(authorized, message, null);
	}

	public AuthorizationStatus(boolean authorized, String message, AuthorizationStatusDenialReason denialReason) {
		this.authorized = authorized;
		this.message = message;
		this.denialReason = denialReason;
	}
	
	public boolean getAuthorized() {
		return authorized;
	}
	public String getMessage() {
		return message;
	}
	public AuthorizationStatusDenialReason getDenialReason() {
		return denialReason;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AuthorizationStatus that = (AuthorizationStatus) o;
		return authorized == that.authorized &&
				Objects.equals(message, that.message) &&
				denialReason == that.denialReason;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authorized, message, denialReason);
	}

	@Override
	public String toString() {
		return "AuthorizationStatus{" +
				"authorized=" + authorized +
				", message='" + message + '\'' +
				", denialReason=" + denialReason +
				'}';
	}
}
