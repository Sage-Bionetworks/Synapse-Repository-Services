package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

/**
 *
 */
public interface OAuthClientManager {
	
	/*
	 * The oauth 'client id' repeesenting Synapse itself
	 */
	public static final String SYNAPSE_OAUTH_CLIENT_ID = "0";
	
	/**
	 * Create a new Open ID Connect client.
	 * 
	 * @param userInfo
	 * @param oauthClient
	 * @return the Id and secret of the newly created client
	 */
	OAuthClient createOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) throws ServiceUnavailableException;

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
	OAuthClient updateOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) throws ServiceUnavailableException;

	/**
	 * Delete an Open ID Connect client.
	 * userInfo param must be the creator of the client (or else a Synapse administrator)
	 * @param userInfo
	 * @param id
	 */
	void deleteOpenIDConnectClient(UserInfo userInfo, String id);
	
	
	/**
	 * Generate and return the secret for the given client.
	 * Note:  Only the hash of the secret is stored.
	 * @param clientId
	 * @return The new secret
	 */
	OAuthClientIdAndSecret createClientSecret(UserInfo userInfo, String clientId);
	
	/**
	 * Validate the client ID secret pair
	 * @param idAndSecret
	 * @return true iff the credentials are valid
	 */
	boolean validateClientCredentials(OAuthClientIdAndSecret idAndSecret);

}
