package org.sagebionetworks.repo.model.dbo.auth;

import java.util.Date;
import java.util.Objects;

public class OIDCAccessTokenData {

	private String tokenId;
	private Long principalId;
	private Long clientId;
	private Long refreshTokenId;
	private Date createdOn;
	private Date expiresOn;
	private String sessionId;
	
	public OIDCAccessTokenData() {
	}

	public String getTokenId() {
		return tokenId;
	}

	public OIDCAccessTokenData setTokenId(String tokenId) {
		this.tokenId = tokenId;
		return this;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public OIDCAccessTokenData setPrincipalId(Long principalId) {
		this.principalId = principalId;
		return this;
	}

	public Long getClientId() {
		return clientId;
	}

	public OIDCAccessTokenData setClientId(Long clientId) {
		this.clientId = clientId;
		return this;
	}

	public Long getRefreshTokenId() {
		return refreshTokenId;
	}

	public OIDCAccessTokenData setRefreshTokenId(Long refreshTokenId) {
		this.refreshTokenId = refreshTokenId;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public OIDCAccessTokenData setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Date getExpiresOn() {
		return expiresOn;
	}

	public OIDCAccessTokenData setExpiresOn(Date expiresOn) {
		this.expiresOn = expiresOn;
		return this;
	}
	
	public String getSessionId() {
		return sessionId;
	}
		
	public OIDCAccessTokenData setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientId, createdOn, expiresOn, principalId, refreshTokenId, sessionId, tokenId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof OIDCAccessTokenData)) {
			return false;
		}
		OIDCAccessTokenData other = (OIDCAccessTokenData) obj;
		return Objects.equals(clientId, other.clientId) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(expiresOn, other.expiresOn) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(refreshTokenId, other.refreshTokenId) && Objects.equals(sessionId, other.sessionId)
				&& Objects.equals(tokenId, other.tokenId);
	}

	@Override
	public String toString() {
		return "OIDCAccessTokenData [tokenId=" + tokenId + ", principalId=" + principalId + ", clientId=" + clientId + ", refreshTokenId="
				+ refreshTokenId + ", createdOn=" + createdOn + ", expiresOn=" + expiresOn + ", sessionId=" + sessionId + "]";
	}
	
}
