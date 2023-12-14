package org.sagebionetworks.repo.manager.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

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
	private String userInfoUrl;
	
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
		this.userInfoUrl = oidcConfig.getUserInfoEndpoint();
	}
	
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return new OpenIdApi(authUrl, tokenUrl, userInfoUrl).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, OIDC_SCOPES, null));
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		if (redirectUrl == null) {
			throw new IllegalArgumentException("RedirectUrl cannot be null");
		}
		try {
			OpenIdService service = (new OpenIdApi(authUrl, tokenUrl, userInfoUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, redirectUrl, null, null, null));
			/*
			 * Get an access token from Google using the provided authorization code.
			 * This token is used to sign request for user's information.
			 */
			Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));
			// Use the access token to get the UserInfo from Google.
			return service.getUserInfo(accessToken);
		} catch(OAuthException e) {
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
