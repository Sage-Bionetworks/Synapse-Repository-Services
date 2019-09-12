package org.sagebionetworks.repo.manager.oauth;

/*
 * This introduces some strong typing, just to help the controller-level serialization
 * machinery know it's handling a JWT.
 */
public class JWTWrapper {
	private String jwt;

	public JWTWrapper(String jwt) {
		super();
		this.jwt = jwt;
	}

	public String getJwt() {
		return jwt;
	}

	public void setJwt(String jwt) {
		this.jwt = jwt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jwt == null) ? 0 : jwt.hashCode());
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
		JWTWrapper other = (JWTWrapper) obj;
		if (jwt == null) {
			if (other.jwt != null)
				return false;
		} else if (!jwt.equals(other.jwt))
			return false;
		return true;
	}

}
