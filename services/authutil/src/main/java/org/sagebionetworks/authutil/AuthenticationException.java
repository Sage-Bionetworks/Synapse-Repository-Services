package org.sagebionetworks.authutil;


@SuppressWarnings("serial")
@Deprecated
public class AuthenticationException extends Exception {
	private String authURL;
	private int respStatus;
	
	public AuthenticationException(int respStatus, String userMessage, Throwable t) {
		super(userMessage, t);
		this.respStatus = respStatus;
	}
	
	public AuthenticationException(String authURL, int respStatus, String userMessage, Throwable t) {
		this(respStatus, userMessage, t);
		this.authURL=authURL;
	}

	public String getAuthURL() {
		return authURL;
	}

	public void setAuthURL(String authURL) {
		this.authURL = authURL;
	}

	public int getRespStatus() {
		return respStatus;
	}

	public void setRespStatus(int respStatus) {
		this.respStatus = respStatus;
	}

}
