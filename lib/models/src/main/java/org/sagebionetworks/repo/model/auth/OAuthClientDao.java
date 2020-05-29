package org.sagebionetworks.repo.model.auth;

import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
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
	 * Store the salted hash of the client secret.
	 * @param clientId
	 * @param newEtag
	 * @param secretHash
	 */
	public void setOAuthClientSecretHash(String clientId, String secretHash, String newEtag);
	
	/**
	 * 
	 * @param clientId
	 * @return
	 * @throws NotFoundException If a client with the given id does not exist or if a secret 
	 * 	for the client was not generated
	 */
	public byte[] getSecretSalt(String clientId) throws NotFoundException;
	
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
	
	/**
	 * @param clientId
	 * @return True if the client with the given id is verified, false otherwise
	 * @throws NotFoundException If a client with the given id does not exist
	 */
	boolean isOauthClientVerified(String clientId) throws NotFoundException;

	/**
	 * Retrieve all clients that a specified user has granted long-lived access to via OAuth refresh tokens.
	 * Does not include clients whose access has expired or has been revoked.
	 * @param userId The user who has delegated access to clients via OAuth 2 refresh tokens
	 * @param nextPageToken For pagination
	 * @param maxLeaseLengthInDays The maximum lease length of a refresh token.
	 *                                Clients having no refresh tokens used after this point in time are
	 *                                omitted from the results.
	 * @return a paginated list of clients which have been granted refresh token(s) for the given user.
	 */
	OAuthClientAuthorizationHistoryList getAuthorizedClientHistory(String userId, String nextPageToken, Long maxLeaseLengthInDays);
}
