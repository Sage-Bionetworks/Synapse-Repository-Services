package org.sagebionetworks.repo.manager.oauth;

import java.util.List;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
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
	OAuthRefreshTokenAndMetadata createRefreshToken(String userId, String clientId, List<OAuthScope> scopes, OIDCClaimsRequest claims);

	/**
	 * Rotates a refresh token, returning the new token and its metadata.
	 * This retrieval locks the row so that it is not changed until the transaction completes.
	 * Therefore, the caller must use {@link org.sagebionetworks.repo.transactions.WriteTransaction}.
	 * @param refreshToken the unhashed refresh token
	 * @return the new token and token metadata
	 * @throws IllegalArgumentException if the passed refresh token does not match an existing refresh token
	 */
	OAuthRefreshTokenAndMetadata rotateRefreshToken(String refreshToken) throws IllegalArgumentException;

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
	 * @param clientId the verified client ID of the client making this request
	 * @param tokenId
	 * @return
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the client
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadata(String clientId, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Retrieve an OAuth 2.0 refresh token's metadata using the tokens itself, on behalf of a client
	 * @param verifiedClientId the verified client ID of the client making this request
	 * @param refreshToken the unhashed token
	 * @return
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the client
	 */
	OAuthRefreshTokenInformation getRefreshTokenMetadataWithToken(String verifiedClientId, String refreshToken) throws NotFoundException, UnauthorizedException;

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
	 * Revoke all refresh tokens between a particular user and a particular client. Also rescinds all consent records, so a user
	 * will have to explicitly authorize the client to issue further tokens.
	 * @param userInfo
	 * @param clientId
	 */
	void revokeRefreshTokensForUserClientPair(UserInfo userInfo, String clientId);

	/**
	 * Revoke a particular refresh token using a token ID, checking a user's credentials to see if they are authorized to revoke it
	 *
	 * @param userInfo
	 * @param tokenId
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the user
	 */
	void revokeRefreshToken(UserInfo userInfo, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Revoke a particular refresh token using a token ID, checking a client's ID to see if they are authorized to revoke it
	 *
	 * @param clientId
	 * @param tokenId
	 * @throws NotFoundException if the token does not exist
	 * @throws UnauthorizedException if the token is not associated with the user
	 */
	void revokeRefreshToken(String clientId, String tokenId) throws NotFoundException, UnauthorizedException;

	/**
	 * Updates a token's metadata.
	 *
	 * @param userInfo
	 * @param metadata
	 * @return
	 */
	OAuthRefreshTokenInformation updateRefreshTokenMetadata(UserInfo userInfo, OAuthRefreshTokenInformation metadata) throws NotFoundException, UnauthorizedException;

	/**
	 * Determine whether a refresh token is active using its unique ID
	 * @param refreshTokenId
	 * @return true if the token is active, false if the token is revoked or doesn't exist
	 */
	public boolean isRefreshTokenActive(String refreshTokenId);
}
