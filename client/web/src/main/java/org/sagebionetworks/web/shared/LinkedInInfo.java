package org.sagebionetworks.web.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class LinkedInInfo implements IsSerializable {

	// The authorization URL to redirect a user to for LinkedIn login
	private String authUrl;
	// The secret for the requestToken
	private String requestSecret;
	// An exception, if it occurs
	private Exception exception;
	
	public LinkedInInfo() { }
	
	public LinkedInInfo(String authUrl, String requestSecret, Exception exception) {
		this.authUrl = authUrl;
		this.requestSecret = requestSecret;
		this.exception = exception;
	}
	
	public String getAuthUrl() {
		return authUrl;
	}
	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}
	public String getRequestSecret() {
		return requestSecret;
	}
	public void setRequestSecret(String requestSecret) {
		this.requestSecret = requestSecret;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authUrl == null) ? 0 : authUrl.hashCode());
		result = prime * result
				+ ((requestSecret == null) ? 0 : requestSecret.hashCode());
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
		LinkedInInfo other = (LinkedInInfo) obj;
		if (authUrl == null) {
			if (other.authUrl != null)
				return false;
		} else if (!authUrl.equals(other.authUrl))
			return false;
		if (requestSecret == null) {
			if (other.requestSecret != null)
				return false;
		} else if (!requestSecret.equals(other.requestSecret))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "LinkedInInfo [authUrl=" + authUrl + ", requestSecret="
				+ requestSecret + ", exception=" + exception + "]";
	}
	
		
}
