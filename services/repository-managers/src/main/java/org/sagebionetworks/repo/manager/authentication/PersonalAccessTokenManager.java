package org.sagebionetworks.repo.manager.authentication;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.web.NotFoundException;

public interface PersonalAccessTokenManager {

	/**
	 * Issues a signed JWT that provides scoped access to a user's account.
	 * @param userInfo
	 * @param request
	 * @return
	 */
	AccessTokenGenerationResponse issueToken(UserInfo userInfo, String accessToken, AccessTokenGenerationRequest request, String oauthEndpoint);

	/**
	 * Determine if a personal access token is active or not.
	 * @param tokenId the unique ID of a token
	 * @return true if the token is active, false if the token has expired, has been revoked, or does not exist
	 */
	boolean isTokenActive(String tokenId);

	/**
	 * Updates the "last used" time for a token to the current time.
	 * @param tokenId
	 */
	void updateLastUsedTime(String tokenId);

	/**
	 * Retrieves a paginated list of personal access tokens.
	 * @param userInfo
	 * @return
	 */
	AccessTokenRecordList getTokenRecords(UserInfo userInfo, String nextPageToken);

	/**
	 * Retrieves metadata for a particular token.
	 * @param userInfo
	 * @param tokenId
	 * @return
	 */
	AccessTokenRecord getTokenRecord(UserInfo userInfo, String tokenId) throws NotFoundException;

	/**
	 * Revokes a personal access token.
	 * @param userInfo
	 * @param tokenId
	 */
	void revokeToken(UserInfo userInfo, String tokenId);
}
