package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;

/**
 * Simple POJO to hold an OAuth refresh token and its ID
 */
public class OAuthRefreshTokenAndMetadata {
	private String refreshToken;
	private OAuthRefreshTokenInformation metadata;


	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public OAuthRefreshTokenInformation getMetadata() {
		return metadata;
	}

	public void setMetadata(OAuthRefreshTokenInformation metadata) {
		this.metadata = metadata;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((metadata == null) ? 0 : metadata.hashCode());
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
		OAuthRefreshTokenAndMetadata other = (OAuthRefreshTokenAndMetadata) obj;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		if (refreshToken == null) {
			if (other.refreshToken != null)
				return false;
		} else if (!refreshToken.equals(other.refreshToken))
			return false;
		return true;
	}
}
