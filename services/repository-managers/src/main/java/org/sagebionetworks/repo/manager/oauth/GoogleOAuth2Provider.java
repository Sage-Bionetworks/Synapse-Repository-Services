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

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s&prompt=select_account";
    private static final String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";

    private static final String MESSAGE = " Message: ";
	private static final String FAILED_PREFIX = "Failed to get User's information from Google. Code: ";
	private static final String GOOGLE_OAUTH_USER_INFO_API_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
	/*
	 * Json keys for object returned by https://www.googleapis.com/oauth2/v2/userinfo
	 */
	public static final String EMAIL = "email";
	public static final String VERIFIED_EMAIL = "verified_email";
	public static final String ID = "id";
	public static final String GIVEN_NAME = "given_name";
	public static final String FAMILY_NAME = "family_name";

	/*
	 * Email scope indicates to Google that we want to request the user's email
	 * after authentication.
	 */
	private static final String SCOPE_EMAIL = "email";

	private String apiKey;
	private String apiSecret;
	
	/**
	 * Thread safe Google provider.
	 * 
	 * @param apiKey Client ID provided by Google developer console.
	 * @param apiSecret Client Secret provided by Google developer console..
	 */
	public GoogleOAuth2Provider(String apiKey, String apiSecret) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}
	
	// remove either buildService or the call to Google2Api() for consistency.  Ditto with ORCID2Api
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return new OAuth2Api(AUTHORIZE_URL, TOKEN_URL).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, SCOPE_EMAIL, null));
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		if (redirectUrl == null) {
			throw new IllegalArgumentException("RedirectUrl cannot be null");
		}
		try {
			OAuthService service = (new OAuth2Api(AUTHORIZE_URL, TOKEN_URL)).
					createService(new OAuthConfig(apiKey, apiSecret, redirectUrl, null, null, null));
			/*
			 * Get an access token from Google using the provided authorization code.
			 * This token is used to sign request for user's information.
			 */
			Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));
			// Use the access token to get the UserInfo from Google.
			OAuthRequest request = new OAuthRequest(Verb.GET, GOOGLE_OAUTH_USER_INFO_API_URL);
			service.signRequest(accessToken, request);
			Response reponse = request.send();
			if(!reponse.isSuccessful()){
				throw new UnauthorizedException(FAILED_PREFIX+reponse.getCode()+MESSAGE+reponse.getMessage());
			}
			return parserResponseBody(reponse.getBody());
		} catch(OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}
	
	/**
	 * Parse the Response from https://www.googleapis.com/oauth2/v2/userinfo.
	 * @param body
	 * @return
	 */
	public static ProvidedUserInfo parserResponseBody(String body){
		try {
			JSONObject json = new JSONObject(body);
			
			ProvidedUserInfo info = new ProvidedUserInfo();
			if(json.has(FAMILY_NAME)){
				info.setLastName(json.getString(FAMILY_NAME));
			}
			if(json.has(GIVEN_NAME)){
				info.setFirstName(json.getString(GIVEN_NAME));
			}
			if(json.has(ID)){
				info.setProvidersUserId(json.getString(ID));
			}
			if(json.has(VERIFIED_EMAIL)){
				boolean isEmailVerfied = json.getBoolean(VERIFIED_EMAIL);
				if(isEmailVerfied){
					if(json.has(EMAIL)){
						info.setUsersVerifiedEmail(json.getString(EMAIL));
					}
				}
			}
			return info;
		} catch (JSONException e) {
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
