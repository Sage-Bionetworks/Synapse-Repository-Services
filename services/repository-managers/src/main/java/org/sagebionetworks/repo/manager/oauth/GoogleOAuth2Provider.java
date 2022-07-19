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

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=%s&redirect_uri=%s&prompt=select_account";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private static final String MESSAGE = " Message: ";
	private static final String FAILED_PREFIX = "Failed to get User's information from Google. Code: ";
	private static final String GOOGLE_OAUTH_USER_INFO_API_URL = "https://openidconnect.googleapis.com/v1/userinfo";
	/*
	 * Json keys for object returned by https://openidconnect.googleapis.com/v1/userinfo
	 */
	public static final String EMAIL = "email";
	public static final String VERIFIED_EMAIL = "email_verified";
	public static final String GIVEN_NAME = "given_name";
	public static final String FAMILY_NAME = "family_name";	
	public static final String SUB = "sub";

	/*
	 * To be OIDC compliant we need the openid scope (See https://developers.google.com/identity/protocols/oauth2/openid-connect#sendauthrequest)
	 */
	private static final String OIDC_SCOPES = "openid profile email";

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
	
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return new OAuth2Api(AUTHORIZE_URL, TOKEN_URL).
				getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, OIDC_SCOPES, null));
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
			return parseUserInfo(reponse.getBody());
		} catch(OAuthException e) {
			throw new UnauthorizedException(e);
		}
	}
	
	/**
	 * Parse the Response from https://www.googleapis.com/oauth2/v2/userinfo.
	 * @param body
	 * @return
	 */
	public static ProvidedUserInfo parseUserInfo(String body) {
		try {
			JSONObject json = new JSONObject(body);

			ProvidedUserInfo info = new ProvidedUserInfo();
			if (json.has(FAMILY_NAME)) {
				info.setLastName(json.getString(FAMILY_NAME));
			}
			if (json.has(GIVEN_NAME)) {
				info.setFirstName(json.getString(GIVEN_NAME));
			}
			if (json.has(SUB)) {
				info.setSubject(json.getString(SUB));
			}
			if (json.has(VERIFIED_EMAIL) && json.getBoolean(VERIFIED_EMAIL) && json.has(EMAIL)) {
				info.setUsersVerifiedEmail(json.getString(EMAIL));
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
