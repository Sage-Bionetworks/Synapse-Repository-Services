package org.sagebionetworks.repo.model.dbo.principal;

import java.util.Optional;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public interface PrincipalOIDCBindingDao {
	
	void bindPrincipalToSubject(Long principalId, OAuthProvider provider, String subject);
	
	Optional<Long> findBindingForSubject(OAuthProvider provider, String subject);
	
	void truncateAll();

}
