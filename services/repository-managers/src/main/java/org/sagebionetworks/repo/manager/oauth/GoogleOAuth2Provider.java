package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.util.ValidateArgument;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verifier;

/**
 * Google OAuth 2.0 implementation of OAuthProvider.
 * 
 * This class requires an apiKey and apiSecret provided by the <a
 * href="https://console.developers.google.com/project">Google developer
 * console</a>.
 * 
 * @author John
 * 
 */
public class GoogleOAuth2Provider implements OAuthProviderBinding {

	private static final String AUTH_URL_DEFAULT_PARAMS = "?response_type=code&client_id=%s&redirect_uri=%s&prompt=select_account";
	
	/*
	 * To be OIDC compliant we need the openid scope (See https://developers.google.com/identity/protocols/oauth2/openid-connect#sendauthrequest)
	 */
	private static final String OIDC_SCOPES = "openid profile email";

	private String apiKey;
	private String apiSecret;
	private String authUrl;
	private String tokenUrl;
	
	/**
	 * Thread safe Google provider.
	 * 
	 * @param apiKey Client ID provided by Google developer console.
	 * @param apiSecret Client Secret provided by Google developer console..
	 */
	public GoogleOAuth2Provider(String apiKey, String apiSecret, OIDCConfig oidcConfig) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.authUrl = oidcConfig.getAuthorizationEndpoint() + AUTH_URL_DEFAULT_PARAMS;
		this.tokenUrl = oidcConfig.getTokenEndpoint();
	}
	
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return new OAuth2Api(authUrl, tokenUrl).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, OIDC_SCOPES, null));
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		ValidateArgument.required(authorizationCode, "The authorizationCode");
		ValidateArgument.required(redirectUrl, "The redirectUrl");
		
		try {
			OAuth2Service service = (new OAuth2Api(authUrl, tokenUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, redirectUrl, null, null, null));
			
			AccessTokenResponse accessTokenResponse = service.getAccessToken(null, new Verifier(authorizationCode));
						
			return accessTokenResponse.parseIdToken();
		} catch (OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}

	@Override
	public AliasAndType retrieveProvidersId(String authorizationCode, String redirectUrl) {
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Google OAuth provider.");
	}

	@Override
	public AliasType getAliasType() {
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Google OAuth provider.");
	}
}
