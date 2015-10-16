package org.sagebionetworks.repo.manager.oauth;

import java.util.Map;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;

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
	public void setLoginProviderMap(Map<OAuthProvider, OAuthProviderBinding> providerMap) {
		this.providerMap = providerMap;
	}

	@Override
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl) {
		return getProviderBinding(provider).getAuthorizationUrl(redirectUrl);
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		return getProviderBinding(provider).validateUserWithProvider(authorizationCode, redirectUrl);
	}
	
	@Override
	public AliasType getAliasTypeForProvider(OAuthProvider provider) {
		return getProviderBinding(provider).getAliasType();
	}
	
	@Override
	public String retrieveProvidersId(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		return getProviderBinding(provider).retrieveProvidersId(authorizationCode, redirectUrl);
	}
	
	@Override
	public OAuthProviderBinding getProviderBinding(OAuthProvider provider){
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
