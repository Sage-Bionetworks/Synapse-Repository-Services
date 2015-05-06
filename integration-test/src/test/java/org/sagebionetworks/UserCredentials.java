package org.sagebionetworks;

public class UserCredentials {
	private String username;
	private Long principalId;
	private String sessionToken;
	
	public UserCredentials(String username, Long principalId,
			String sessionToken) {
		super();
		this.username = username;
		this.principalId = principalId;
		this.sessionToken = sessionToken;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public Long getPrincipalId() {
		return principalId;
	}
	
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
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
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((sessionToken == null) ? 0 : sessionToken.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
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
		UserCredentials other = (UserCredentials) obj;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (sessionToken == null) {
			if (other.sessionToken != null)
				return false;
		} else if (!sessionToken.equals(other.sessionToken))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}
