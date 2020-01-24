package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.manager.UserAuthorization;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;

/**
 *
 */
public interface OpenIDConnectManager {
	/**
	 * 
	 * @param authorizationRequest
	 * @return
	 */
	OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(OIDCAuthorizationRequest authorizationRequest);
	
	/**
	 * 
	 * @param userInfo
	 * @param authorizationRequest
	 * @return
	 */
	OAuthAuthorizationResponse authorizeClient(UserInfo userInfo, OIDCAuthorizationRequest authorizationRequest);
	
	/**
	 * 
	 * @param authorizationCode
	 * @param verifiedClientId Client ID verified via client authentication
	 * @param redirectUri
	 * @param issuer
	 * @return
	 */
	OIDCTokenResponse getAccessToken(String authorizationCode, String verifiedClientId, String redirectUri, String issuer);
	
	/**
	 * Given an OAuth access token with an audience and subject, 'decode' the
	 * subject to find the Synapse user id and return it
	 * @param accessToken
	 * @return
	 */
	String getUserId(String accessToken);
	
	/**
	 * Parse the given JWT token and return the user identity, groups,
	 * and scopes/claims authorized by the token.
	 * 
	 * @param oauthToken
	 * @return
	 */
	public UserAuthorization getUserAuthorization(String oauthToken);
	
	/**
	 * Given the validated access token content, return the up-to-date user info
	 * requested in the scopes / claims embedded in the access token
	 * 
	 * @param userAuthorization
	 * @param oauthClientId
	 * @param oauthEndpoint
	 * @return either a JWT or a JSON Object, depending on whether the client registered a value for
	 * userinfo_signed_response_alg
	 */
	Object getUserInfo(UserAuthorization userAuthorization, String oauthClientId, String oauthEndpoint);

}
