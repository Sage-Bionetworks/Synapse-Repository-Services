package org.sagebionetworks.controller.annotations.model;

import org.sagebionetworks.repo.model.oauth.OAuthScope;


import java.util.List;
import java.util.Objects;


public class RequiredScopeModel {
	private List<OAuthScope> oAuthScopes;

	public List<OAuthScope> getOAuthScopes() {
		return oAuthScopes;
	}

	public RequiredScopeModel withOAuthScopes(List<OAuthScope> oAuthScopes) {
		this.oAuthScopes = oAuthScopes;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(oAuthScopes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequiredScopeModel other = (RequiredScopeModel) obj;
		return Objects.equals(((RequiredScopeModel) obj).oAuthScopes, other.oAuthScopes);
	}

	@Override
	public String toString() {
		return "RequiredScopeModel [oAuthScopes=" + oAuthScopes + "]";
	}
}
