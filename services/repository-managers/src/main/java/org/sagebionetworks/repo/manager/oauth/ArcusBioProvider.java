package org.sagebionetworks.repo.manager.oauth;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.util.ValidateArgument;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verifier;

/**
 * OAuthProvider for Arcus Bio's OpenID Connect server
 * 
 */
public class ArcusBioProvider implements OAuthProviderBinding {

	private static final String AUTH_URL_DEFAULT_PARAMS = "?response_type=code&client_id=%s&redirect_uri=%s";
	
	/*
	 * To be OIDC compliant we need the openid scope.
	 */
	private static final String OIDC_SCOPES = "openid email";
	
	private static final String TOKEN = "token";
	private static final String USER_INFO = "userinfo";
	

	private String apiKey;
	private String apiSecret;
	private String authUrl;
	String tokenUrl;
	String userInfoUrl;
	
	/**
	 * Thread safe OAuth 2.0 provider.
	 * 
	 * @param apiKey Client ID provided by Arcus Bio.
	 * @param apiSecret Client Secret provided by Arcus Bio.
	 */
	public ArcusBioProvider(String apiKey, String apiSecret, OIDCConfig oidcConfig) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.authUrl = oidcConfig.getAuthorizationEndpoint() + AUTH_URL_DEFAULT_PARAMS;
		this.tokenUrl = oidcConfig.getTokenEndpoint();
		// ArcusBio uses Okta, which doesn't include the userinfo endpoint in its Discovery document
		// it appears the endpoint is the same as the token endpoint but with the
		// suffix 'token' replaced with 'userinfo'
		if (tokenUrl.indexOf(TOKEN)<0) {
			throw new IllegalStateException("Expected to find '"+TOKEN+"' in "+this.tokenUrl);
		}
		this.userInfoUrl = tokenUrl.replace(TOKEN, USER_INFO);
	}
	
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return new OAuth2Api(authUrl, tokenUrl, userInfoUrl).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, OIDC_SCOPES, null));
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		ValidateArgument.required(authorizationCode, "The authorizationCode");
		ValidateArgument.required(redirectUrl, "The redirectUrl");
		
		try {
			OAuth2Service service = (new OAuth2Api(authUrl, tokenUrl, userInfoUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, redirectUrl, null, null, null));
			
			AccessTokenResponse accessTokenResponse = service.getAccessToken(null, new Verifier(authorizationCode));
						
			ProvidedUserInfo providedUserInfo =  accessTokenResponse.parseIdToken();
			// if the IdP fails to provide the required claims in the ID Token, then
			// a request is made to the /userinfo endpoint
			if (StringUtils.isEmpty(providedUserInfo.getUsersVerifiedEmail())) {
				providedUserInfo = service.getUserInfo(accessTokenResponse.getToken());
			}
			return providedUserInfo;
		} catch (OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}

	@Override
	public AliasAndType retrieveProvidersId(String authorizationCode, String redirectUrl) {
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Arcus Bio OAuth provider.");
	}

	@Override
	public AliasType getAliasType() {
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Arcus Bio OAuth provider.");
	}
}
