package org.sagebionetworks.repo.manager;

/**
 * Holds the result of an authorization check.
 * If 'authorized' is false then 'reason' gives the user-presentable reason why
 * 
 * @author brucehoff
 *
 */
public class AuthorizationStatus {
	private boolean authorized;
	private String reason;
	
	
	public AuthorizationStatus(boolean authorized, String reason) {
		this.authorized = authorized;
		this.reason = reason;
	}
	
	public boolean getAuthorized() {
		return authorized;
	}
	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}

}
