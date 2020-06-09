package org.sagebionetworks.repo.manager.oauth;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * The OAuthRefreshToken manager is used to create, retrieve, and manipulate the refresh tokens that grant OAuth clients
 * long-lived access to user resources.
 *
 * See also the {@link OpenIDConnectManager}.
 */
public interface OAuthRefreshTokenManager {

	/**
	 * Creates an OAuth 2.0 refresh token, which can be used to grant an OAuth client long-lived, revocable access
	 * to a user's resources
	 *
	 * @param userId
	 * @param clientId the client authorized to access the resources
	 * @param scopes the {@link OAuthScope}s that the client is authorized to access with.
	 * @param claims the OIDC claims that have been granted to the client. the {@link OIDCClaimsRequest} object also
	 *                 specifies when that information is contained in the OIDC id_token, or returned when the client
	 *                 makes a request at the userinfo endpoint
	 * @return a generated refresh token, and the refresh token's unique ID.
	 */
	OAuthRefreshTokenAndId createRefreshToken(String userId, String clientId, List<OAuthScope> scopes, OIDCClaimsRequest claims);

	/**
	 * Retrieves token metadata, if it exists, using the unhashed token and associated {@link org.sagebionetworks.repo.model.oauth.OAuthClient} ID.
	 * This retrieval locks the row so that it may be updated. Therefore, a caller must use a {@link org.sagebionetworks.repo.transactions.WriteTransaction}.
	 * @param token the unhashed token
	 * @param clientId the associated client ID
	 * @return an {@link Optional} containing the token metadata, if it exists. Otherwise, an empty optional.
	 */
	Optional<OAuthRefreshTokenInformation> getRefreshTokenMetadataForUpdate(String token, String clientId);

	/**
	 * Updates the hash for a refresh token and returns the new hash. Metadata (such as the token name) will be retained,
	 * but certain fields may be updated (such as the Etag and the Last Used date).
	 *
	 * Prior to calling this method, the row should be locked with {@link #getRefreshTokenMetadataForUpdate}. Thus, an exception will
	 * be thrown if calling this method unless already in a {@link org.sagebionetworks.repo.transactions.WriteTransaction}
	 * @param tokenId
	 * @return
	 */
	String rotateRefreshToken(String tokenId);

	/**
	 * Retrieve an OAuth 2.0 refresh token's metadata using the tokens' ID, on behalf of a user
	 * @param userInfo
	 * @param tokenId
	 * @return
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the user
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadata(UserInfo userInfo, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Retrieve an OAuth 2.0 refresh token's metadata using the tokens' ID, on behalf of a client
	 * @param clientId
	 * @param tokenId
	 * @return
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the client
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadata(String clientId, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Retrieve a paginated record of which clients are currently granted OAuth 2.0 access to a user via active refresh tokens
	 * @param userInfo the user whose records should be retrieved
	 * @param nextPageToken {@link org.sagebionetworks.repo.model.NextPageToken} token for pagination
	 * @return
	 */
	OAuthClientAuthorizationHistoryList getAuthorizedClientHistory(UserInfo userInfo, String nextPageToken);

	/**
	 * Retrieve a paginated list of metadata for active refresh tokens between a particular user and client
	 *
	 * @param userInfo
	 * @param clientId
	 * @param nextPageToken {@link org.sagebionetworks.repo.model.NextPageToken} token for pagination
	 * @return
	 */
	OAuthRefreshTokenInformationList getMetadataForActiveRefreshTokens(UserInfo userInfo, String clientId, String nextPageToken);

	/**
	 * Revoke all refresh tokens between a particular user and a particular client.
	 * @param userInfo
	 * @param clientId
	 */
	void revokeRefreshTokensForUserClientPair(UserInfo userInfo, String clientId);

	/**
	 * Revoke a particular refresh token using a token ID
	 *
	 * @param userInfo
	 * @param tokenId
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the user
	 */
	void revokeRefreshToken(UserInfo userInfo, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Revokes a refresh token using the token itself. This method is usually invoked by an OAuth client, but a client
	 * ID is not required because if this is called by an unauthorized party, the token should be revoked anyways.
	 * @param clientId
	 * @param revocationRequest
	 */
	void revokeRefreshToken(String clientId, OAuthTokenRevocationRequest revocationRequest) throws NotFoundException;

	/**
	 * Updates a token metadata.
	 *
	 * @param userInfo
	 * @param metadata
	 * @return
	 */
	OAuthRefreshTokenInformation updateRefreshTokenMetadata(UserInfo userInfo, OAuthRefreshTokenInformation metadata) throws NotFoundException, UnauthorizedException;
}
