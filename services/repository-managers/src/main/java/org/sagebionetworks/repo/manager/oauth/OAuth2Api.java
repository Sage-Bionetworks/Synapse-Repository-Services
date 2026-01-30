package org.sagebionetworks.repo.manager.oauth;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.exceptions.OAuthException;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

/**
 * Google OAuth2.0 
 * Released under the same license as scribe (MIT License)
 * @author yincrash
 * 
 * @see <a href="https://gist.githubusercontent.com/yincrash/2465453/raw/9d4eb3149ff8c0eba0316a29d4598949975ac6f5/Google2APi.java">Original Google2Apis</a>
 * 
 * 
 */
public class OAuth2Api extends DefaultApi20 {
	private static final String ACCESS_TOKEN_TAG = "access_token";
	private static final String ID_TOKEN_TAG = "id_token";
	private static final String ERROR_TAG = "error";
		
	public static final String EMAIL = "email";
	public static final String EMAIL_VERIFIED = "email_verified";
	public static final String GIVEN_NAME = "given_name";
	public static final String FAMILY_NAME = "family_name";	
	public static final String SUB = "sub";
	
	String authorizationEndpoint;
	String accessTokenEndpoint;
	String userInfoEndpoint;
	
	public OAuth2Api(String authorizationEndpoint, String accessTokenEndpoint, String userInfoEndpoint) {
		this.authorizationEndpoint = authorizationEndpoint;
		this.accessTokenEndpoint = accessTokenEndpoint;
		this.userInfoEndpoint = userInfoEndpoint;
	}
	
    @Override
    public String getAccessTokenEndpoint() {
    	return accessTokenEndpoint;
    }
        
    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return (String response) -> {
			Preconditions.checkEmptyString(response, "Response body is incorrect. Can't extract a token from an empty string");
			try {
				JSONObject json = new JSONObject(response);
				if (json.has(ACCESS_TOKEN_TAG)) {
					String accessToken = OAuthEncoder.decode(json.getString(ACCESS_TOKEN_TAG));
					String idToken = json.has(ID_TOKEN_TAG) ? json.getString(ID_TOKEN_TAG) : null;
					return new AccessTokenResponse(accessToken, idToken, response);
				} else if (json.has(ERROR_TAG)) {
					throw new OAuthException(json.getString(ERROR_TAG));
				} else {
					throw new OAuthException("Response body is incorrect. Can't parse: '" + response + "'", null);
				}
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
		};
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        // Append scope if present
        if (config.hasScope()) {
            String scopedAuthorizationUrl = authorizationEndpoint + "&scope=%s";
            return String.format(scopedAuthorizationUrl, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()),
                    OAuthEncoder.encode(config.getScope()));
        } else {
            return String.format(authorizationEndpoint, config.getApiKey(),
                    OAuthEncoder.encode(config.getCallback()));
        }
    }
    
    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }
    
    @Override
    public OAuth2Service createService(OAuthConfig config) {
        return new BasicOAuth2Service(this, config);
    }
        
    static ProvidedUserInfo parseUserInfo(String body) {
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
			if (json.has(EMAIL)) {
				info.setEmail(json.getString(EMAIL));
			}
			if (json.has(EMAIL_VERIFIED)) {
				info.setEmailVerified(json.getString(EMAIL_VERIFIED));
			}
			if (json.has(EMAIL_VERIFIED) && json.has(EMAIL)) {
				try {
					if (json.getBoolean(EMAIL_VERIFIED)) {
						info.setUsersVerifiedEmail(json.getString(EMAIL));
					}
				} catch (JSONException e) {
					// don't set the value
				}
			}
			return info;
		} catch (JSONException e) {
			throw new UnauthorizedException(e);
		}
    }
    	
    private class BasicOAuth2Service extends OAuth20ServiceImpl implements OAuth2Service {

        private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
        private static final String GRANT_TYPE = "grant_type";

        private OAuth2Api api;
        private OAuthConfig config;

        public BasicOAuth2Service(OAuth2Api api, OAuthConfig config) {
            super(api, config);
            this.api = api;
            this.config = config;
        }
                
        @Override
        public AccessTokenResponse getAccessToken(Token requestToken, Verifier verifier) {
            OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
            switch (api.getAccessTokenVerb()) {
            case POST:
                request.addBodyParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                request.addBodyParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                request.addBodyParameter(OAuthConstants.CODE, verifier.getValue());
                if (config.getCallback()!=null) request.addBodyParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                request.addBodyParameter(GRANT_TYPE, GRANT_TYPE_AUTHORIZATION_CODE);
                break;
            case GET:
            default:
                request.addQuerystringParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
                request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
                request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
                if (config.getCallback()!=null) request.addQuerystringParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
                if(config.hasScope()) request.addQuerystringParameter(OAuthConstants.SCOPE, config.getScope());
            }
            Response response = request.send();
            return (AccessTokenResponse) api.getAccessTokenExtractor().extract(response.getBody());
        }

		@Override
		public ProvidedUserInfo getUserInfo(String accessToken) {
			OAuthRequest request = new OAuthRequest(Verb.GET, userInfoEndpoint);
			request.addHeader("Authorization", "Bearer "+accessToken);
			Response response = request.send();
			if (!response.isSuccessful()) {
				throw new UnauthorizedException("Failed to get user's information from provider (Code: " + response.getCode() + ", Message: " + response.getMessage() + ")");
			}
			return parseUserInfo(response.getBody());
		}


    }

}
