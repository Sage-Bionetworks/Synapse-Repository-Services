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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (authorized ? 1231 : 1237);
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
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
		if (reason == null) {
			if (other.reason != null)
				return false;
		} else if (!reason.equals(other.reason))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AuthorizationStatus [authorized=" + authorized + ", reason="
				+ reason + "]";
	}

}
