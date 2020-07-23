package org.sagebionetworks.repo.model.auth;

import java.util.Date;

import org.sagebionetworks.repo.web.NotFoundException;

public interface PersonalAccessTokenDao {

	/**
	 * Retrieve the token record/metadata, if the record exists.
	 * @param tokenId the unique ID of the token record to retrieve
	 * @throws NotFoundException if the token does not exist
	 */
	AccessTokenRecord getTokenRecord(String tokenId) throws NotFoundException;

	/**
	 * Get the last used date of the token, if the record exists.
	 * @param tokenId the unique ID of the token record to retrieve
	 * @throws NotFoundException if the token does not exist
	 */
	Date getLastUsedDate(String tokenId) throws NotFoundException;


	/**
	 * Create a record for a personal access token. Note that the actual token is a signed JWT, and this record merely
	 * indicates that the token has not been explicitly revoked.
	 * @param metadata token information to store, excluding the ID field
	 * @return the new token record
	 */
	AccessTokenRecord createTokenRecord(AccessTokenRecord metadata);

	/**
	 * Get a paginated list of token records for a user.
	 * @param userId the user who owns the tokens
	 * @param nextPageToken pagination token
	 * @return a paginated list of personal access token records belonging to the specified user
	 */
	AccessTokenRecordList getTokenRecords(String userId, String nextPageToken);

	/**
	 * Set the "last used" time for a token to the current time.
	 * @param tokenId
	 */
	void updateLastUsed(String tokenId);

	/**
	 * Deletes a token record by its unique token ID. This effectively revokes the token.
	 * @param tokenId
	 */
	void deleteToken(String tokenId);

	/**
	 * Deletes the least-recently used personal access tokens for a particular user, if the number of tokens is over the limit.
	 * The number of remaining active personal access tokens will be the specified limit.
	 * @param userId
	 * @param maxNumberOfTokens the maximum
	 */
	void deleteLeastRecentlyUsedTokensOverLimit(String userId, Long maxNumberOfTokens);

}
