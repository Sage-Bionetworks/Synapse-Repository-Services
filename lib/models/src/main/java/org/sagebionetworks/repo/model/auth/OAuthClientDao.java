package org.sagebionetworks.repo.model.auth;

import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;

public interface OAuthClientDao {
	
	/**
	 * 
	 * @param client the metadata for the OAuth Client
	 * @param secret the shared secret used to authenticate the client
	 * @return the id of the newly created OAuth client
	 */
	public String createOAuthClient(OAuthClient client, String secret);
	
	/**
	 * 
	 * @param clientId
	 * @return the selected OAuth Client, omitting the shared secret
	 */
	public OAuthClient getOAuthClient(String clientId);
	
	/**
	 * 
	 * @param nextPageToken
	 * @param createdBy
	 * @return a paginated list of OAuth clients created by the given user
	 */
	public OAuthClientList listOAuthClients(String nextPageToken, Long createdBy);
	
	/**
	 * 
	 * @param clientId
	 * @return the shared secret for the given clientId
	 */
	public String getOAuthClientSecret(String clientId);
	
	/**
	 * Update the indiecated OAuth Client
	 * clientID, clientSecret, createdBy, createdOn are never changed
	 * @param client
	 * @return the updated object
	 */
	public OAuthClient updateOAuthClient(OAuthClient client);
	
	/**
	 * Delete the indicated OAuth Client
	 * @param clientId
	 */
	public void deleteOAuthClient(String clientId);

	/**
	 * 
	 * @param clientId
	 * @return the SectorIdentifier for the client
	 */
	public SectorIdentifier getSectorIdentifier(String clientId);
	
	/**
	 * Delete the unique sector identifier having the given URI
	 * Can only be done if no OAuth Client uses it
	 * @param sectorIdentiferUri
	 */
	public void deleteSectorIdentifer(String sectorIdentiferUri);
	
	
}
