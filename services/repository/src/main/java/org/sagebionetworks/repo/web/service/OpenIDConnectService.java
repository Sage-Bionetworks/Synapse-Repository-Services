package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
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
}
