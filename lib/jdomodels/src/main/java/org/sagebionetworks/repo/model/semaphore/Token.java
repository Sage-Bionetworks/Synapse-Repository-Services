package org.sagebionetworks.repo.model.semaphore;

/**
 * Token used to track a semaphore lock.
 *
 */
public class Token {
	
	String token;
	long expiresTimeMs;
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public long getExpiresTimeMs() {
		return expiresTimeMs;
	}
	public void setExpiresTimeMs(long expiresTimeMs) {
		this.expiresTimeMs = expiresTimeMs;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (expiresTimeMs ^ (expiresTimeMs >>> 32));
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		Token other = (Token) obj;
		if (expiresTimeMs != other.expiresTimeMs)
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Token [token=" + token + ", expiresTimeMs=" + expiresTimeMs
				+ "]";
	}

}
