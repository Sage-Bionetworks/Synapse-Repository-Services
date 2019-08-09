package org.sagebionetworks.auth.services;

import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;

public interface OpenIDConnectService {
	
	/**
	 * 
	 * @param userId
	 * @param oauthClient
	 * @return
	 */
	public OAuthClient createOpenIDConnectClient(Long userId, OAuthClient oauthClient);
	
	/**
	 * 
	 * @param userId
	 * @param id
	 * @return
	 */
	public OAuthClient getOpenIDConnectClient(Long userId, String id);
	

	/**
	 * 
	 * @param userId
	 * @param nextPageToken
	 * @return
	 */
	public OAuthClientList listOpenIDConnectClients(Long userId, String nextPageToken);
	
	/**
	 * 
	 * @param userId
	 * @param oauthClient
	 * @return
	 */
	public OAuthClient updateOpenIDConnectClient(Long userId, OAuthClient oauthClient);
	
	/**
	 * 
	 * @param userId
	 * @param id
	 */
	public void deleteOpenIDConnectClient(Long userId, String id);
	
	/**
	 * 
	 * @return the OIDC Discovery Document
	 */
	public OIDConnectConfiguration getOIDCConfiguration();
	
	/**
	 * Return the public keys used to validate OIDC JSON Web Token signatures
	 */
	public JsonWebKeySet getOIDCJsonWebKeySet();
	
	/**
	 * 
	 * @param authorizationRequest
	 * @return
	 */
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(OIDCAuthorizationRequest authorizationRequest);
	
	/**
	 * Authorize OAuth client for the requested scope and return an authorization code
	 * @param userId
	 * @param authorizationRequest
	 * @return authorization code
	 */
	public OAuthAuthorizationResponse authorizeClient(Long userId,  OIDCAuthorizationRequest authorizationRequest);
	
	public OIDCTokenResponse getTokenResponse(OAuthGrantType grantType, String code, String redirectUri, String refreshToken, String scope, String claims);
		
	public Object getUserInfo(String accessToken);
}
