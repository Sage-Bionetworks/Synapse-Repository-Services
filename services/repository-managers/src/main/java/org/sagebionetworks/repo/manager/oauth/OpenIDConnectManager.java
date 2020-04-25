package org.sagebionetworks.repo.manager.oauth;

import org.apache.commons.codec.digest.DigestUtils;
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
	 * @return
	 */
	OIDCTokenResponse getAccessToken(String authorizationCode, String verifiedClientId, String redirectUri, String oauthEndpoint);
	
	/**
	 * Given an OAuth access token with an audience and subject, 'decode' the
	 * subject to find the Synapse user id and return it
	 * @param accessToken
	 * @return
	 */
	String getUserId(String accessToken);
	
	/**
	 * Return true iff the specified user has already granted consent for the given client, socpe and claims
	 * @param userInfo
	 * @param authorizationRequest
	 * @return
	 */
	boolean hasUserGrantedConsent(UserInfo userInfo, OIDCAuthorizationRequest authorizationRequest);
	
	/**
	 * Given the validated access token content, return the up-to-date user info
	 * requested in the scopes / claims embedded in the access token
	 * 
	 * @param accessToken
	 * @return either a JWT or a JSON Object, depending on whether the client registered a value for
	 * userinfo_signed_response_alg
	 */
	Object getUserInfo(String accessToken, String oauthEndpoint);
	
	/**
	 * 
	 * @param authorizationRequest
	 * @return a hash of the critical fields
	 */
	public static String getScopeHash(OIDCAuthorizationRequest authorizationRequest) {
		return DigestUtils.sha256Hex(authorizationRequest.getScope()+authorizationRequest.getClaims());
	}
	


}
