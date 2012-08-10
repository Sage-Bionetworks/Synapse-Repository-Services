package org.sagebionetworks.authutil;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Session {
	private String sessionToken;
	
	public Session() {}

	public Session(String token) {
		super();
		this.sessionToken = token;
	}

	public String getSessionToken() {
		return sessionToken;
	}
	public void setSessionToken(String token) {
		this.sessionToken = token;
	}
	
	public String toString() {return "sessionToken="+getSessionToken();}
}
