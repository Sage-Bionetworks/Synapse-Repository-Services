package org.sagebionetworks.repo.model.dbo.auth;

public interface OAuthAccessTokenDao {

	/**
	 * Store the data for an access token issued for the given user
	 * 
	 * @param data
	 */
	void storeAccessTokenRecord(OIDCAccessTokenData data);

	/**
	 * @param tokenId The id of the access token
	 * @return True if an access token record with the given token id exists, false otherwise
	 */
	boolean isAccessTokenRecordExists(String tokenId);

	/**
	 * Deletes all the data about every access token issued for the given user
	 * @param userId
	 */
	void deleteAccessTokenRecords(Long userId);
	
	/**
	 * Deletes the record for the access token with the given id
	 * @param tokenId
	 */
	void deleteAccessTokenRecord(String tokenId);
}
