package org.sagebionetworks.repo.model.dbo.principal;

import java.util.Optional;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public interface PrincipalOIDCBindingDao {
	
	void bindPrincipalToSubject(Long principalId, Long aliasId, OAuthProvider provider, String subject);
	
	Optional<PrincipalOidcBinding> findBindingForSubject(OAuthProvider provider, String subject);
	
	void setBindingAlias(Long bindingId, Long aliasId);
	
	void deleteBinding(Long bindingId);
	
	void clearBindings(Long principalId);
	
	void truncateAll();


}
