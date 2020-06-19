package org.sagebionetworks.repo.model.auth;

import java.util.Optional;

import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;

public interface OAuthRefreshTokenDao {

	/**
	 * Determine if a token is active or not.
	 * @param tokenId the unique ID of a token
	 * @param maxLeaseLengthInDays refresh tokens that have been unused for this many days are considered expired
	 * @return true if the token is active, false if the token is revoked or does not exist
	 */
	boolean isTokenActive(String tokenId, Long maxLeaseLengthInDays);

	/**
	 * Update the stored hash for an existing token, This must only be called when the record is locked,
	 * e.g. by calling {@link #getMatchingTokenByHashForUpdate}.
	 *
	 * Note: This method only updates fields that should update on token rotation:
	 * 		- the token hash
	 * 		- the last used date
	 * 		- the etag
	 *
	 * @param tokenInformation metadata of the token to update (updates by ID)
	 * @param newHash the hash to store
	 */
	void updateTokenHash(OAuthRefreshTokenInformation tokenInformation, String newHash);

	/**
	 * Retrieve a matching token, if it exists.
	 * @param hash The hash of the token
	 * @return an {@link Optional} that contains the token metadata, if it exists.
	 */
	Optional<OAuthRefreshTokenInformation>  getMatchingTokenByHash(String hash);

	/**
	 * Retrieve a matching token, if it exists. Locks the row until released in an existing transaction.
	 * @param hash The hash of the token
	 * @return an {@link Optional} that contains the token metadata, if it exists.
	 */
	Optional<OAuthRefreshTokenInformation>  getMatchingTokenByHashForUpdate(String hash);

	/**
	 * Retrieve the refresh token metadata, if that refresh token exists
	 * @param tokenId the unique ID of the token to retrieve
	 * @return an {@link Optional} containing the refresh token, if it exists
	 */
	Optional<OAuthRefreshTokenInformation> getRefreshTokenMetadata(String tokenId);

	/**
	 * Store a refresh token
	 * @param hashedToken the token hash to store
	 * @param metadata token information to store, including the client and user associated with the token
	 * @return the stored refresh token
	 */
	OAuthRefreshTokenInformation createRefreshToken(String hashedToken, OAuthRefreshTokenInformation metadata);

	/**
	 * Update the metadata for a refresh token based on the specified token ID
	 * Fields that can be updated with this method are:
	 * - {@link OAuthRefreshTokenInformation#getName()} ()}
	 * - {@link OAuthRefreshTokenInformation#getModifiedOn()} ()}
	 * - {@link OAuthRefreshTokenInformation#getEtag()}
	 *
	 * @param metadata the object to update
	 */
	void updateRefreshTokenMetadata(OAuthRefreshTokenInformation metadata);

	/**
	 * Get the active refresh tokens between a user and a client
	 * @param userId the user whose resources can be accessed using refresh tokens
	 * @param clientId the client whom can use tokens to access resources
	 * @param nextPageToken pagination token
	 * @param maxLeaseLengthInDays do not retrieve refresh tokens that have been unused for this many days.
	 * @return a paginated list of active refresh tokens between the specified user and client
	 */
	OAuthRefreshTokenInformationList getActiveTokenInformation(String userId, String clientId, String nextPageToken, Long maxLeaseLengthInDays);

	/**
	 * Deletes a token by its unique token ID
	 * @param tokenId
	 */
	void deleteToken(String tokenId);

	/**
	 * Delete all refresh tokens associated with both the specified user and specified client
	 * @param userId
	 * @param clientId
	 */
	void deleteAllTokensForUserClientPair(String userId, String clientId);
	
	/**
	 * Deletes the least-recently used active refresh tokens between a particular user and particular client, if the client has more tokens than the limit.
	 * The number of remaining active refresh tokens will be the specified limit.
	 * @param userId
	 * @param clientId
	 */
	void deleteLeastRecentlyUsedTokensOverLimit(String userId, String clientId, Long maxNumberOfTokens);

}
