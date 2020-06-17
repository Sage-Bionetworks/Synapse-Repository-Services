package org.sagebionetworks.repo.manager.oauth;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.web.NotFoundException;

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
	 * Obtain an access token, and conditionally an ID token and/or refresh token using an authorization code
	 * @param authorizationCode
	 * @param verifiedClientId Client ID verified via client authentication
	 * @param redirectUri
	 * @return
	 */
	OIDCTokenResponse generateTokenResponseWithAuthorizationCode(String authorizationCode, String verifiedClientId, String redirectUri, String oauthEndpoint);

	/**
	 * Obtain an access token and a new refresh token, and conditionally an ID token using a refresh token
	 * @param refreshToken
	 * @param verifiedClientId Client ID verified via client authentication
	 * @param scope
	 * @param oauthEndpoint
	 * @return
	 */
	OIDCTokenResponse generateTokenResponseWithRefreshToken(String refreshToken, String verifiedClientId, String scope, String oauthEndpoint);

	/**
	 * Given an OAuth access token with an audience and subject, 'decode' the
	 * subject to find the Synapse user id and return it.
	 * @param accessToken
	 * @return the user ID of the Synapse user referred to by the access token
	 * @throws UnauthorizedException if an associated refresh token is expired
	 */
	String validateAccessToken(String accessToken) throws UnauthorizedException;

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

	/**
	 * Revokes a refresh token using the token itself. This method is usually invoked by an OAuth client, but a client
	 * ID is not required because if this is called by an unauthorized party, the token should be revoked anyways.
	 * @param verifiedClientId
	 * @param revocationRequest
	 */
	void revokeToken(String verifiedClientId, OAuthTokenRevocationRequest revocationRequest) throws NotFoundException;


}
