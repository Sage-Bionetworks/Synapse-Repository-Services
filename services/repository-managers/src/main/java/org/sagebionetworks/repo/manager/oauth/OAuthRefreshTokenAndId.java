package org.sagebionetworks.repo.manager.oauth;

/**
 * Simple POJO to hold an OAuth refresh token and its ID
 */
public class OAuthRefreshTokenAndId {
	private String refreshToken;
	private String tokenId;


	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tokenId == null) ? 0 : tokenId.hashCode());
		result = prime * result
				+ ((refreshToken == null) ? 0 : refreshToken.hashCode());
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
		OAuthRefreshTokenAndId other = (OAuthRefreshTokenAndId) obj;
		if (tokenId == null) {
			if (other.tokenId != null)
				return false;
		} else if (!tokenId.equals(other.tokenId))
			return false;
		if (refreshToken == null) {
			if (other.refreshToken != null)
				return false;
		} else if (!refreshToken.equals(other.refreshToken))
			return false;
		return true;
	}
}
