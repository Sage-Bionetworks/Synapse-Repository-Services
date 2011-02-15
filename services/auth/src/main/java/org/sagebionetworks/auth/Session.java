package org.sagebionetworks.auth;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Session {
	private String sessionToken;
	private String displayName;
	
	public Session() {}

	public Session(String token, String displayName) {
		super();
		this.sessionToken = token;
		this.displayName = displayName;
	}

	public String getSessionToken() {
		return sessionToken;
	}
	public void setSessionToken(String token) {
		this.sessionToken = token;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String toString() {return "sessionToken="+getSessionToken()+", displayName="+getDisplayName();}
}
