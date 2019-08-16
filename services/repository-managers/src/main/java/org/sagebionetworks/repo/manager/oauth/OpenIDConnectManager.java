package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;

import com.nimbusds.jwt.JWT;

/**
 *
 */
public interface OpenIDConnectManager {
	
	/**
	 * Create a new Open ID Connect client.
	 * 
	 * @param userInfo
	 * @param oauthClient
	 * @return the Id and secret of the newly created client
	 */
	OAuthClientIdAndSecret createOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient);

	/**
	 * 
	 * Retrieve the metadata for an OpenID client given its ID.
	 *
	 * @param userInfo must be the creator or a Synapse administrator
	 * @param id id of the OAuth client to be retrieved.
	 * @return
	 */
	OAuthClient getOpenIDConnectClient(UserInfo userInfo, String id);

	/**
	 * List the Open ID Connect clients created by the user represented by the userInfo param
	 * @param userInfo
	 * @param nextPageToken
	 * @return
	 */
	OAuthClientList listOpenIDConnectClients(UserInfo userInfo, String nextPageToken);

	/**
	 * Update the metadata for an Open ID Connect client.  userInfo param must be the
	 * creator of the client (or else a Synapse administrator)
	 * @param userInfo
	 * @param oauthClient
	 * @return
	 */
	OAuthClient updateOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient);

	/**
	 * Delete an Open ID Connect client.
	 * userInfo param must be the creator of the client (or else a Synapse administrator)
	 * @param userInfo
	 * @param id
	 */
	void deleteOpenIDConnectClient(UserInfo userInfo, String id);
	
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
	OIDCTokenResponse getAccessToken(String authorizationCode, String verifiedClientId, String redirectUri);
	
	/**
	 * 
	 * Given the validated access token content, return the up-to-date user info
	 * requested in the scopes / claims embedded in the access token

	 * @param accessToken
	 * @return either a JWT or a JSON Object, depending on whether the client registered a value for
	 * userinfo_signed_response_alg
	 */
	Object getUserInfo(JWT accessToken);

}
