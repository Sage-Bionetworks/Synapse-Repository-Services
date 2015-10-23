package org.sagebionetworks.repo.manager.oauth;

import java.util.Map;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

/**
 * Simple implementation of the  OAuthManager.
 * @author John
 *
 */
public class OAuthManagerImpl implements OAuthManager {
	
	private Map<OAuthProvider, OAuthProviderBinding> providerMap;
	
	/**
	 * Injected.
	 */
	public void setProviderMap(Map<OAuthProvider, OAuthProviderBinding> providerMap) {
		this.providerMap = providerMap;
	}

	@Override
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl) {
		return getBinding(provider).getAuthorizationUrl(redirectUrl);
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		return getBinding(provider).validateUserWithProvider(authorizationCode, redirectUrl);
	}
	
	@Override
	public AliasAndType retrieveProvidersId(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		return getBinding(provider).retrieveProvidersId(authorizationCode, redirectUrl);
	}
	
	@Override
	public OAuthProviderBinding getBinding(OAuthProvider provider){
		if(provider == null){
			throw new IllegalArgumentException("OAuthProvider cannot be null");
		}
		OAuthProviderBinding binding = providerMap.get(provider);
		if(binding == null){
			throw new IllegalArgumentException("Unknown provider: "+provider.name());
		}
		return binding;
	}

}
