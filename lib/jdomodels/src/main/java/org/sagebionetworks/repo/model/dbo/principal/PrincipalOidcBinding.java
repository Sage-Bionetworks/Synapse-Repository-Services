package org.sagebionetworks.repo.model.dbo.principal;

import java.util.Objects;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public class PrincipalOidcBinding {
	
	private Long bindingId;
	private OAuthProvider provider;
	private String subject;
	private Long userId;
	private Long aliasId;

	public PrincipalOidcBinding() {
		
	}
	
	public Long getBindingId() {
		return bindingId;
	}
	
	public PrincipalOidcBinding setBindingId(Long bindingId) {
		this.bindingId = bindingId;
		return this;
	}

	public OAuthProvider getProvider() {
		return provider;
	}

	public PrincipalOidcBinding setProvider(OAuthProvider provider) {
		this.provider = provider;
		return this;
	}

	public String getSubject() {
		return subject;
	}

	public PrincipalOidcBinding setSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public Long getUserId() {
		return userId;
	}

	public PrincipalOidcBinding setUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public Long getAliasId() {
		return aliasId;
	}

	public PrincipalOidcBinding setAliasId(Long aliasId) {
		this.aliasId = aliasId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliasId, bindingId, provider, subject, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PrincipalOidcBinding)) {
			return false;
		}
		PrincipalOidcBinding other = (PrincipalOidcBinding) obj;
		return Objects.equals(aliasId, other.aliasId) && Objects.equals(bindingId, other.bindingId) && provider == other.provider
				&& Objects.equals(subject, other.subject) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "PrincipalOidcBinding [bindingId=" + bindingId + ", provider=" + provider + ", subject=" + subject + ", userId=" + userId
				+ ", aliasId=" + aliasId + "]";
	}

}
