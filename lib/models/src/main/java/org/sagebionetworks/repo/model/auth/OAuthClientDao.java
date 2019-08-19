package org.sagebionetworks.repo.model.auth;

import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.web.NotFoundException;

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
	 * Create a Sector Identifier with URI and secret.  The URI must be unique across all Sector Identifiers
	 * @param sectorIdentifier
	 * @return the id of the new SectorIdentifer
	 */
	public String createSectorIdentifier(SectorIdentifier sectorIdentifier);
	
	/**
	 * Find whether there is a Sector Identifier for the given URI.
	 * @param uri
	 * @return true iff there is already a Sector Identifier for the given URI
	 */
	public boolean doesSectorIdentifierExistForURI(String uri);

	/**
	 * 
	 * @param clientId
	 * @return the encryption secret for the SectorIdentifier for the given client client
	 * @throws NotFoundException if there is no Sector Identifier for the given client ID
	 */
	public String getSectorIdentifierSecretForClient(String clientId) throws NotFoundException;
	
	/**
	 * Delete the unique sector identifier having the given URI
	 * Can only be done if no OAuth Client uses it
	 * @param sectorIdentiferUri
	 */
	public void deleteSectorIdentifer(String sectorIdentiferUri);
	
	
}
