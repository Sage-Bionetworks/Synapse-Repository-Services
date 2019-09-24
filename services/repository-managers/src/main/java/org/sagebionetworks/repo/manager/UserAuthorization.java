package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

public class UserAuthorization {
	private List<OAuthScope> scopes;
	private Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims;
	private String oauthClientId;
	private UserInfo userInfo;
	public List<OAuthScope> getScopes() {
		return scopes;
	}
	public void setScopes(List<OAuthScope> scopes) {
		this.scopes = scopes;
	}
	public Map<OIDCClaimName, OIDCClaimsRequestDetails> getOidcClaims() {
		return oidcClaims;
	}
	public void setOidcClaims(Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims) {
		this.oidcClaims = oidcClaims;
	}
	public String getOauthClientId() {
		return oauthClientId;
	}
	public void setOauthClientId(String oauthClientId) {
		this.oauthClientId = oauthClientId;
	}
	public UserInfo getUserInfo() {
		return userInfo;
	}
	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((oauthClientId == null) ? 0 : oauthClientId.hashCode());
		result = prime * result + ((oidcClaims == null) ? 0 : oidcClaims.hashCode());
		result = prime * result + ((scopes == null) ? 0 : scopes.hashCode());
		result = prime * result + ((userInfo == null) ? 0 : userInfo.hashCode());
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
		UserAuthorization other = (UserAuthorization) obj;
		if (oauthClientId == null) {
			if (other.oauthClientId != null)
				return false;
		} else if (!oauthClientId.equals(other.oauthClientId))
			return false;
		if (oidcClaims == null) {
			if (other.oidcClaims != null)
				return false;
		} else if (!oidcClaims.equals(other.oidcClaims))
			return false;
		if (scopes == null) {
			if (other.scopes != null)
				return false;
		} else if (!scopes.equals(other.scopes))
			return false;
		if (userInfo == null) {
			if (other.userInfo != null)
				return false;
		} else if (!userInfo.equals(other.userInfo))
			return false;
		return true;
	}
	
	
	


}
