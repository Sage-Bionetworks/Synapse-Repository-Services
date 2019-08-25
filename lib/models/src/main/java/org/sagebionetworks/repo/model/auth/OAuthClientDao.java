package org.sagebionetworks.repo.model.auth;

import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface OAuthClientDao {
	
	/**
	 * 
	 * @param client the metadata for the OAuth Client
	 * @return the id of the newly created OAuth client
	 */
	public OAuthClient createOAuthClient(OAuthClient client);
	
	/**
	 * 
	 * @param clientId
	 * @return the selected OAuth Client, omitting the shared secret
	 */
	public OAuthClient getOAuthClient(String clientId);
	
	/**
	 * 
	 * @param clientId
	 * @return
	 */
	public OAuthClient selectOAuthClientForUpdate(String clientId);
	
	/**
	 * 
	 * @param nextPageToken
	 * @param createdBy
	 * @return a paginated list of OAuth clients created by the given user
	 */
	public OAuthClientList listOAuthClients(String nextPageToken, Long createdBy);
	
	/**
	 * Update the indicated OAuth Client
	 * clientID, clientSecret, createdBy, createdOn are never changed
	 * @param client
	 * @return the updated object
	 */
	public OAuthClient updateOAuthClient(OAuthClient client);
	
	/**
	 * 
	 * @param clientId
	 * @return
	 */
	public String getOAuthClientCreator(String clientId);
	
	/**
	 * Delete the indicated OAuth Client
	 * @param clientId
	 */
	public void deleteOAuthClient(String clientId);
	
	/**
	 * 
	 * @param clientId
	 */
	public void setOAuthClientVerified(String clientId);
	
	/**
	 * Store the salted hash of the client secret.
	 * @param clientId
	 * @param secretHash
	 */
	public void setOAuthClientSecretHash(String clientId, String secretHash);
	
	/**
	 * 
	 * @param clientId
	 * @return
	 */
	public byte[] getSecretSalt(String clientId);
	
	/**
	 * 
	 * @param clientId
	 * @return true iff the provided password hash is correct
	 */
	public boolean checkOAuthClientSecretHash(String clientId, String secretHash);
	
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
