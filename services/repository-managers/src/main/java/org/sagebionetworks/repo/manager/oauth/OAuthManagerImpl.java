package org.sagebionetworks.repo.manager.oauth;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.springframework.stereotype.Service;

/**
 * Simple implementation of the  OAuthManager.
 * @author John
 *
 */
@Service
public class OAuthManagerImpl implements OAuthManager {
	
	private static final Logger LOGGER = LogManager.getLogger(OAuthManagerImpl.class);
	
	private Map<OAuthProvider, OAuthProviderBinding> oauthProvidersBindingMap;
	
	public OAuthManagerImpl(Map<OAuthProvider, OAuthProviderBinding> oauthProvidersBindingMap) {
		this.oauthProvidersBindingMap = oauthProvidersBindingMap;
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
		try {
			return getBinding(provider).validateUserWithProvider(authorizationCode, redirectUrl);
		} catch (UnauthorizedException e) {
			// UnauthorizedException trace is not logged at the controller level, added after https://sagebionetworks.jira.com/browse/SYNSD-1231
			LOGGER.warn("Could not validate user with {} provider (Code: {}, RedirectUrl: {}):", provider, authorizationCode, redirectUrl, e);
			throw e;
		}
	}
	
	@Override
	public AliasAndType retrieveProvidersId(OAuthProvider provider,
			String authorizationCode, String redirectUrl) {
		try {
			return getBinding(provider).retrieveProvidersId(authorizationCode, redirectUrl);
		} catch (UnauthorizedException e) {
			// UnauthorizedException trace is not logged at the controller level, added after https://sagebionetworks.jira.com/browse/SYNSD-1231
			LOGGER.warn("Could not retrive user id from {} provider (Code: {}, RedirectUrl: {}):", provider, authorizationCode, redirectUrl, e);
			throw e;
		}
	}
	
	@Override
	public OAuthProviderBinding getBinding(OAuthProvider provider){
		if(provider == null){
			throw new IllegalArgumentException("OAuthProvider cannot be null");
		}
		OAuthProviderBinding binding = oauthProvidersBindingMap.get(provider);
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
