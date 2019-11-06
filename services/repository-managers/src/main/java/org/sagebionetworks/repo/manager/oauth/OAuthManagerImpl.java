package org.sagebionetworks.repo.manager.oauth;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
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
	public void setProviderMap(Map<OAuthProvider, OAuthProviderBinding> providerMap) {
		this.providerMap = providerMap;
	}

	@Override
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl, String state) {
		String authURL = getBinding(provider).getAuthorizationUrl(redirectUrl);
		if (StringUtils.isEmpty(state)) {
			return authURL;
		} else {
			try {
				return new URIBuilder(authURL).addParameter("state", state).build().toString();
			} catch (URISyntaxException e) {
				throw new RuntimeException("Failed to add state "+state+" to url "+authURL, e);
			}
		}
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

	@Override
	public AliasType getAliasTypeForProvider(OAuthProvider provider) {
		return getBinding(provider).getAliasType();
	}

}
