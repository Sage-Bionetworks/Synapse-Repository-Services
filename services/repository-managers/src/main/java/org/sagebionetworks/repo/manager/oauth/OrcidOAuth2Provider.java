package org.sagebionetworks.repo.manager.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.util.ValidateArgument;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verifier;

public class OrcidOAuth2Provider implements OAuthProviderBinding {

	private static final String AUTH_URL_DEFAULT_PARAMS = "?response_type=code&client_id=%s&redirect_uri=%s";

	// See https://info.orcid.org/ufaqs/what-is-an-oauth-scope-and-which-scopes-does-orcid-support/
	private static final String SCOPE_OPENID = "openid"; 
	
	public static final String ORCID = "orcid";

	private String apiKey;
	private String apiSecret;
	private String authUrl;
	private String tokenUrl;
	
	public OrcidOAuth2Provider(String apiKey, String apiSecret, OIDCConfig oidcConfig) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.authUrl = oidcConfig.getAuthorizationEndpoint() + AUTH_URL_DEFAULT_PARAMS;
		this.tokenUrl = oidcConfig.getTokenEndpoint();
	}

	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return  new OAuth2Api(authUrl, tokenUrl).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, SCOPE_OPENID, null));
	}
	
	private static final String ORCID_URI_PREFIX = "https://orcid.org/";
	
	private static String convertOrcIdToURI(String orcid) {
		return ORCID_URI_PREFIX + orcid;
	}

	@Override
	public AliasAndType retrieveProvidersId(String authorizationCode, String redirectUrl) {
		return validateUserWithProvider(authorizationCode, redirectUrl).getAliasAndType();
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
		ValidateArgument.required(authorizationCode, "The authorizationCode");
		ValidateArgument.required(redirectUrl, "The redirectUrl");
		
		try {
			OAuth2Service service = (new OAuth2Api(authUrl, tokenUrl)).
					createService(new OAuthConfig(apiKey, apiSecret, null, null, null, null));

			AccessTokenResponse accessTokenResponse = service.getAccessToken(null, new Verifier(authorizationCode));
			
			ProvidedUserInfo userInfo = accessTokenResponse.parseIdToken();

			// We also get the orcid from the token response body to construct the alias
			// See https://github.com/ORCID/ORCID-Source/blob/main/orcid-web/ORCID_AUTH_WITH_OPENID_CONNECT.md
			String orcid = parseOrcidId(accessTokenResponse.getRawResponse());
			
			userInfo.setAliasAndType(new AliasAndType(convertOrcIdToURI(orcid), AliasType.USER_ORCID));
			
			return userInfo;
		} catch (OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}

	@Override
	public AliasType getAliasType() {
		return AliasType.USER_ORCID;
	}


}
