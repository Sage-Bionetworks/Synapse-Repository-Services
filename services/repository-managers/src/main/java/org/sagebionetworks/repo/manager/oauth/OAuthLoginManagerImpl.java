package org.sagebionetworks.repo.manager.oauth;

import java.util.Map;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

/**
 * Simple implementation of the  OAuthManager.
 * @author John
 *
 */
public class OAuthLoginManagerImpl implements OAuthAuthenticationManager, OAuthLoginManager {
	
	private Map<OAuthProvider, OAuthAuthenticationProviderBinding> authenticationProviderMap;
	
	private Map<OAuthProvider, OAuthLoginProviderBinding> loginProviderMap;
	
	/**
	 * Injected.
	 */
	public void setProviderMap(Map<OAuthProvider, OAuthLoginProviderBinding> providerMap) {
		this.loginProviderMap = providerMap;
	}

	@Override
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl) {
		return getAuthenticationProviderBinding(provider).getAuthorizationUrl(redirectUrl);
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		return getLoginProviderBinding(provider).validateUserWithProvider(authorizationCode, redirectUrl);
	}
	
	@Override
	public OAuthAuthenticationProviderBinding getAuthenticationProviderBinding(OAuthProvider provider){
		if(provider == null){
			throw new IllegalArgumentException("OAuthProvider cannot be null");
		}
		OAuthAuthenticationProviderBinding binding = authenticationProviderMap.get(provider);
		if(binding == null){
			throw new IllegalArgumentException("Unknown provider: "+provider.name());
		}
		return binding;
	}

	@Override
	public OAuthLoginProviderBinding getLoginProviderBinding(OAuthProvider provider){
		if(provider == null){
			throw new IllegalArgumentException("OAuthProvider cannot be null");
		}
		OAuthLoginProviderBinding binding = loginProviderMap.get(provider);
		if(binding == null){
			throw new IllegalArgumentException("Unknown provider: "+provider.name());
		}
		return binding;
	}

}
