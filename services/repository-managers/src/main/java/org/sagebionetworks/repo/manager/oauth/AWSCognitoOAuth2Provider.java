package org.sagebionetworks.repo.manager.oauth;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.util.ValidateArgument;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verifier;
/**
 * OAuthProvider for AWS Cognito-based OpenID Connect server
 * 
 */
public class AWSCognitoOAuth2Provider implements OAuthProviderBinding {

	private static final String AUTH_URL_DEFAULT_PARAMS = "?response_type=code&client_id=%s&redirect_uri=%s";
	
	/*
	 * To be OIDC compliant we need the openid scope.
	 */
	private static final String OIDC_SCOPES = "openid email";


	private String apiKey;
	private String apiSecret;
	private OIDCConfig oidcConfig;
	
	private OAuth2Api getOAuth2Api() {
		String authUrl = oidcConfig.getAuthorizationEndpoint() + AUTH_URL_DEFAULT_PARAMS;
		String tokenUrl = oidcConfig.getTokenEndpoint();;
		String userInfoUrl =  oidcConfig.getUserInfoEndpoint();;
		return new OAuth2Api(authUrl, tokenUrl, userInfoUrl);
	}
	/**
	 * Thread safe OAuth 2.0 provider.
	 * 
	 * @param apiKey Client ID provided by AWS Cognito.
	 * @param apiSecret Client Secret provided by AWS Cognito.
	 */
	public AWSCognitoOAuth2Provider(String apiKey, String apiSecret, OIDCConfig oidcConfig) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.oidcConfig=oidcConfig;
	}
	
	@Override
	public String getAuthorizationUrl(String redirectUrl) {
		return getOAuth2Api().getAuthorizationUrl(new OAuthConfig(apiKey, null, redirectUrl, null, OIDC_SCOPES, null));
	}

	@Override
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
		ValidateArgument.required(authorizationCode, "The authorizationCode");
		ValidateArgument.required(redirectUrl, "The redirectUrl");
		
		try {
			OAuth2Service service = getOAuth2Api().
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
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Cognito OAuth provider.");
	}

	@Override
	public AliasType getAliasType() {
		throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the Cognito OAuth provider.");
	}
}
