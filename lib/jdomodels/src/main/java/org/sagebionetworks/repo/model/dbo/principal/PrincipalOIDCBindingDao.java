package org.sagebionetworks.repo.model.dbo.principal;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public interface PrincipalOIDCBindingDao {
	
	void bindPrincipalToSubject(Long principalId, Long aliasId, OAuthProvider provider, String subject);
	
	Optional<PrincipalOidcBinding> findBindingForSubject(OAuthProvider provider, String subject);
	
	void setBindingAlias(Long bindingId, Long aliasId);
	
	void deleteBinding(Long bindingId);

	/**
	 * Remove any binding(s) for the given principal and provider. This removes the identity link
	 * whether it is alias-backed (e.g. ORCID or Google) or not; it does not remove the underlying
	 * PrincipalAlias.
	 *
	 * @return the number of binding rows removed
	 */
	int deleteBindingForProvider(Long principalId, OAuthProvider provider);

	void clearBindings(Long principalId);

	List<OAuthProvider> getLinkedProviders(Long principalId);

	void truncateAll();


}
