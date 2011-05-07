package org.sagebionetworks.web.shared.users;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class InitiateSession implements IsSerializable {

	private String displayName;
	private String sessionToken;
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getSessionToken() {
		return sessionToken;
	}
	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result
				+ ((sessionToken == null) ? 0 : sessionToken.hashCode());
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
		InitiateSession other = (InitiateSession) obj;
		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName))
			return false;
		if (sessionToken == null) {
			if (other.sessionToken != null)
				return false;
		} else if (!sessionToken.equals(other.sessionToken))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "InitiateSession [displayName=" + displayName
				+ ", sessionToken=" + sessionToken + "]";
	}	
	
}
