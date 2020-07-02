package org.sagebionetworks.repo.manager.oauth;

import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.manager.PrivateFieldUtils;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistory;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthBadRequestException;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OAuthRefreshTokenManagerImpl implements OAuthRefreshTokenManager {

	@Autowired
	OAuthDao oauthDao;

	@Autowired
	OAuthClientDao oauthClientDao;

	@Autowired
	OAuthRefreshTokenDao oauthRefreshTokenDao;

	@Autowired
	Clock clock;

	/**
	 * The maximum number of days a refresh token can go unused and still
	 * be considered active. A refresh token is "used" when it is used
	 * to issue a new access token.
	 */
	private static final Long REFRESH_TOKEN_LEASE_DURATION_DAYS = 180L;

	// The maximum number of refresh tokens that can be issued between a user-client pair.
	private static final Long MAX_REFRESH_TOKENS_PER_CLIENT_PER_USER = 100L;

	public static boolean canViewAndAlterTokenMetadata(UserInfo userInfo, OAuthRefreshTokenInformation metadata) {
		return userInfo.getId().toString().equals(metadata.getPrincipalId()) || userInfo.isAdmin();
	}

	@WriteTransaction
	@Override
	public OAuthRefreshTokenAndMetadata createRefreshToken(String userId, String clientId, List<OAuthScope> scopes, OIDCClaimsRequest claims) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(clientId, "clientId");
		// The OpenIDConnectManager will handle if scope/claims are semantically valid
		// Here, just require that scope be non-null, and the claims maps be non-null
		ValidateArgument.required(scopes, "scopes");
		ValidateArgument.required(claims, "claims");
		ValidateArgument.required(claims.getId_token(), "id_token claims");
		ValidateArgument.required(claims.getUserinfo(), "userinfo claims");

		if (AuthorizationUtils.isUserAnonymous(Long.valueOf(userId))) {
			throw new UnauthorizedException("Anonymous users may not issue OAuth 2.0 refresh tokens.");
		}

		String token = generateRefreshToken();
		String hash = hashToken(token); // Save the hash, not the token

		// Before we create the token, ensure the user/client pair is under the max tokens limit
		oauthRefreshTokenDao.deleteLeastRecentlyUsedTokensOverLimit(userId, clientId, MAX_REFRESH_TOKENS_PER_CLIENT_PER_USER - 1);

		// Create the token
		OAuthRefreshTokenInformation tokenMetadata = new OAuthRefreshTokenInformation();
		tokenMetadata.setPrincipalId(userId);
		tokenMetadata.setClientId(clientId);
		tokenMetadata.setScopes(scopes);
		tokenMetadata.setClaims(claims);
		tokenMetadata.setName(UUID.randomUUID().toString());
		tokenMetadata.setLastUsed(clock.now());
		tokenMetadata.setAuthorizedOn(clock.now());
		tokenMetadata.setModifiedOn(clock.now());
		tokenMetadata.setEtag(UUID.randomUUID().toString());

		tokenMetadata = oauthRefreshTokenDao.createRefreshToken(hash, tokenMetadata);

		// Return the unhashed token and ID
		OAuthRefreshTokenAndMetadata tokenAndId = new OAuthRefreshTokenAndMetadata();
		tokenAndId.setRefreshToken(token);
		tokenAndId.setMetadata(tokenMetadata);
		return tokenAndId;
	}

	@MandatoryWriteTransaction
	@Override
	public OAuthRefreshTokenAndMetadata rotateRefreshToken(String refreshToken) {
		String hash = hashToken(refreshToken);
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(hash)
				.orElseThrow(() -> new IllegalArgumentException("The token does not match an existing token."));

		if (!oauthRefreshTokenDao.isTokenActive(metadata.getTokenId(), REFRESH_TOKEN_LEASE_DURATION_DAYS)) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_grant, "The refresh token has expired.");
		}

		String newToken = generateRefreshToken();
		String newHash = hashToken(newToken);

		// Update the etag, and last used date (not modified on, which tells when the name was last changed)
		metadata.setLastUsed(clock.now());
		metadata.setEtag(UUID.randomUUID().toString());

		oauthRefreshTokenDao.updateTokenHash(metadata, newHash);

		OAuthRefreshTokenInformation updatedMetadata = oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId())
				.orElseThrow(() -> new IllegalStateException("Could not retrieve refresh token metadata after updating it."));

		OAuthRefreshTokenAndMetadata tokenAndMetadata = new OAuthRefreshTokenAndMetadata();
		tokenAndMetadata.setRefreshToken(newToken);
		tokenAndMetadata.setMetadata(updatedMetadata);
		return tokenAndMetadata;

	}

	@Override
	public OAuthRefreshTokenInformation getRefreshTokenMetadata(UserInfo userInfo, String tokenId) throws NotFoundException {
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getRefreshTokenMetadata(tokenId)
				.orElseThrow(() -> new NotFoundException("Refresh token with ID: " + tokenId + " does not exist"));
		if (!canViewAndAlterTokenMetadata(userInfo, metadata)) {
			throw new UnauthorizedException("You do not have permission to view this token metadata");
		}
		return metadata;
	}

	@Override
	public OAuthRefreshTokenInformation getRefreshTokenMetadata(String clientId, String tokenId) throws NotFoundException {
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getRefreshTokenMetadata(tokenId)
				.orElseThrow(() -> new NotFoundException("Refresh token with ID: " + tokenId + " does not exist"));
		if (!clientId.equals(metadata.getClientId())) {
			throw new UnauthorizedException("You do not have permission to view this token metadata");
		}
		return metadata;
	}

	@Override
	public OAuthRefreshTokenInformation getRefreshTokenMetadataWithToken(String verifiedClientId, String refreshToken) throws NotFoundException, UnauthorizedException {
		String hash = hashToken(refreshToken);
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getMatchingTokenByHash(hash)
				.orElseThrow(() -> new NotFoundException("The refresh token does not exist."));

		if (!verifiedClientId.equals(metadata.getClientId())) {
			throw new UnauthorizedException("You do not have permission to view this token metadata");
		}
		return metadata;
	}

	@Override
	public OAuthClientAuthorizationHistoryList getAuthorizedClientHistory(UserInfo userInfo, String nextPageToken) {
		OAuthClientAuthorizationHistoryList results = oauthClientDao.getAuthorizedClientHistory(userInfo.getId().toString(), nextPageToken, REFRESH_TOKEN_LEASE_DURATION_DAYS);
		for (OAuthClientAuthorizationHistory result : results.getResults()) {
			PrivateFieldUtils.clearPrivateFields(result.getClient());
		}
		return results;
	}

	@Override
	public OAuthRefreshTokenInformationList getMetadataForActiveRefreshTokens(UserInfo userInfo, String clientId, String nextPageToken) {
		return oauthRefreshTokenDao.getActiveTokenInformation(userInfo.getId().toString(), clientId, nextPageToken, REFRESH_TOKEN_LEASE_DURATION_DAYS);
	}

	@WriteTransaction
	@Override
	public void revokeRefreshTokensForUserClientPair(UserInfo userInfo, String clientId) {
		ValidateArgument.required(clientId, "clientId");
		oauthDao.deleteAuthorizationConsentForClient(userInfo.getId(), Long.valueOf(clientId));
		oauthRefreshTokenDao.deleteAllTokensForUserClientPair(userInfo.getId().toString(), clientId);
	}

	@WriteTransaction
	@Override
	public void revokeRefreshToken(UserInfo userInfo, String tokenId) {
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getRefreshTokenMetadata(tokenId)
				.orElseThrow(() -> new NotFoundException("Refresh token with ID:" + tokenId + " does not exist"));

		if (!canViewAndAlterTokenMetadata(userInfo, metadata)) {
			throw new UnauthorizedException("The specified token is owned by a different user and cannot be revoked");
		}

		oauthRefreshTokenDao.deleteToken(tokenId);
	}

	@WriteTransaction
	@Override
	public void revokeRefreshToken(String clientId, String tokenId) {
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getRefreshTokenMetadata(tokenId)
				.orElseThrow(() -> new NotFoundException("Refresh token with ID:" + tokenId + " does not exist"));

		if (!clientId.equals(metadata.getClientId())) {
			throw new UnauthorizedException("The specified token could not be revoked. It is owned by a different client.");
		}

		oauthRefreshTokenDao.deleteToken(tokenId);
	}

	@WriteTransaction
	@Override
	public OAuthRefreshTokenInformation updateRefreshTokenMetadata(UserInfo userInfo, OAuthRefreshTokenInformation metadata) {
		ValidateArgument.required(metadata, "OAuthRefreshTokenInformation");
		ValidateArgument.required(metadata.getTokenId(), "id");
		ValidateArgument.required(metadata.getName(), "name");
		ValidateArgument.required(metadata.getEtag(), "etag");

		OAuthRefreshTokenInformation currentMetadata = oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId())
				.orElseThrow(() -> new NotFoundException("The refresh token does not exist."));

		if (!canViewAndAlterTokenMetadata(userInfo, currentMetadata)) {
			throw new UnauthorizedException("You are not authorized to update this token because it belongs to a different user.");
		}

		if (!metadata.getEtag().equals(currentMetadata.getEtag())) {
			throw new ConflictingUpdateException("The refresh token metadata was updated since you last fetched it. Retrieve it again and reapply the update.");
		}

		// The name is currently the only field the user can specify
		currentMetadata.setName(metadata.getName());
		currentMetadata.setModifiedOn(clock.now());
		currentMetadata.setEtag(UUID.randomUUID().toString());

		oauthRefreshTokenDao.updateRefreshTokenMetadata(currentMetadata);

		return oauthRefreshTokenDao.getRefreshTokenMetadata(metadata.getTokenId())
				.orElseThrow(() -> new IllegalStateException("The token metadata could not be retrieved after an update."));
	}

	public boolean isRefreshTokenActive(String refreshTokenId) {
		return oauthRefreshTokenDao.isTokenActive(refreshTokenId, REFRESH_TOKEN_LEASE_DURATION_DAYS);
	}

	/**
	 * Generates a random string suitable for use as a refresh token.
	 * @return a cryptographically-unguessable random string
	 */
	private String generateRefreshToken() {
		return PBKDF2Utils.generateSecureRandomString();
	}

	/**
	 * Hashes a refresh token and returns the SHA256 hash, formatted as a hexadecimal String.
	 * @return the hash of a token.
	 */
	private String hashToken(String token) {
		/*
		 * We don't salt the token before hashing because
		 * 		1. We need to perform lookups with only the token and client ID, requiring either
		 * 				- the use of the same salt for all of a clients tokens, or
		 *				- extracting multiple salts and computing multiple hashes on a lookup
		 * 		2. The token is a randomly generated string with high entropy, so salting provides little security benefit
		 */
		return DigestUtils.sha256Hex(token);
	}
}
