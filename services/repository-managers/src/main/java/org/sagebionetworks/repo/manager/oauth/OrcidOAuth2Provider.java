package org.sagebionetworks.repo.manager.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

public class OrcidOAuth2Provider implements OAuthProviderBinding {

	private static final String AUTH_URL_DEFAULT_PARAMS = "?response_type=code&client_id=%s&redirect_uri=%s";

	// See https://info.orcid.org/ufaqs/what-is-an-oauth-scope-and-which-scopes-does-orcid-support/
	private static final String SCOPE_AUTHENTICATE = "openid"; 
	
	public static final String ORCID = "orcid";

	private String apiKey;
	private String apiSecret;
	private String authUrl;
	private String tokenUrl;
	private String userInfoUrl;
	
	public OrcidOAuth2Provider(String apiKey, String apiSecret, OIDCConfig oidcConfig) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.authUrl = oidcConfig.getAuthorizationEndpoint() + AUTH_URL_DEFAULT_PARAMS;
		this.tokenUrl = oidcConfig.getTokenEndpoint();
		this.userInfoUrl = oidcConfig.getUserInfoEndpoint();
	}


	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return  new OpenIdApi(authUrl, tokenUrl, userInfoUrl).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, SCOPE_AUTHENTICATE, null));
	}
	
	private static final String ORCID_URI_PREFIX = "https://orcid.org/";
	
	private static String convertOrcIdToURI(String orcid) {
		return ORCID_URI_PREFIX + orcid;
	}

	@Override
	public AliasAndType retrieveProvidersId(String authorizationCode, String redirectUrl) {
		try{
			// Note:  We don't need to use the redirectUrl.
			OpenIdService service = (new OpenIdApi(authUrl, tokenUrl, userInfoUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, null, null, null, null));
			/*
			 * Get an access token from ORCID using the provided authorization code.
			 * This token is used to sign request for user's information.
			 */
			Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));
			String orcid = parseOrcidId(accessToken.getRawResponse());
			return new AliasAndType(convertOrcIdToURI(orcid), AliasType.USER_ORCID);
		}catch(OAuthException e){
			throw new UnauthorizedException(e);
		}
	}
	
	/**
	 * Parse the Response from https://api.orcid.org/oauth/token.
	 * 
	 * @param body
	 * @return
	 */
	public static String parseOrcidId(String body){
		try {
			JSONObject json = new JSONObject(body);
			if(json.has(ORCID)){
				return json.getString(ORCID);
			} else {
				throw new RuntimeException("Missing "+ORCID+" from "+body);
			}
		} catch (JSONException e) {
			throw new UnauthorizedException(e);
		}
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		if (redirectUrl == null) {
			throw new IllegalArgumentException("RedirectUrl cannot be null");
		}
		try {
			OpenIdService service = (new OpenIdApi(authUrl, tokenUrl, userInfoUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, null, null, null, null));

			Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));

			return service.getUserInfo(accessToken);
		} catch(OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}

	@Override
	public AliasType getAliasType() {
		return AliasType.USER_ORCID;
	}


}
