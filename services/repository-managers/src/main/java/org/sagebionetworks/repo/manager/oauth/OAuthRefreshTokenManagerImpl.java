package org.sagebionetworks.repo.manager.oauth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.sagebionetworks.repo.manager.PrivateFieldUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistory;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class OAuthRefreshTokenManagerImpl implements OAuthRefreshTokenManager {

	@Autowired
	OAuthClientDao oauthClientDao;

	@Autowired
	OAuthRefreshTokenDao oauthRefreshTokenDao;

	@Autowired
	OIDCTokenHelper oidcTokenHelper;

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
	public OAuthRefreshTokenAndId createRefreshToken(String userId, String clientId, List<OAuthScope> scopes, OIDCClaimsRequest claims) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(clientId, "clientId");
		// The OpenIDConnectManager will handle if scope/claims are semantically valid
		// Here, just require that scope be non-null, and the claims maps be non-null
		ValidateArgument.required(scopes, "scopes");
		ValidateArgument.required(claims, "claims");
		ValidateArgument.required(claims.getId_token(), "id_token claims");
		ValidateArgument.required(claims.getUserinfo(), "userinfo claims");

		String token = generateRefreshToken();
		String hash = hashToken(token); // Save the hash, not the token

		// Before we create the token, ensure the user/client pair is under the max tokens limit
		oauthRefreshTokenDao.deleteLeastRecentlyUsedTokensOverLimit(userId, clientId, MAX_REFRESH_TOKENS_PER_CLIENT_PER_USER);

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
		OAuthRefreshTokenAndId tokenAndId = new OAuthRefreshTokenAndId();
		tokenAndId.setTokenId(tokenMetadata.getTokenId());
		tokenAndId.setRefreshToken(token);
		return tokenAndId;
	}

	@MandatoryWriteTransaction
	@Override
	public Optional<OAuthRefreshTokenInformation> getRefreshTokenMetadataForUpdate(String token, String clientId) throws NotFoundException {
		String hash = hashToken(token);
		return oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(hash, clientId);
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
		// Verify that tokens exist for the pair
		OAuthRefreshTokenInformationList tokens = oauthRefreshTokenDao.getActiveTokenInformation(userInfo.getId().toString(), clientId, null, REFRESH_TOKEN_LEASE_DURATION_DAYS);
		if (tokens.getResults().size() == 0) {
			throw new NotFoundException("Refresh tokens have not been granted to client " + clientId);
		} else {
			oauthRefreshTokenDao.deleteAllTokensForUserClientPair(userInfo.getId().toString(), clientId);
		}
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
	public void revokeRefreshToken(String clientId, OAuthTokenRevocationRequest revocationRequest) {
		String tokenIdToRevoke = null;
		switch (revocationRequest.getToken_type_hint()) {
			case access_token: // retrieve the refresh token ID from the JWT, if it exists
				Jwt<JwsHeader, Claims> token = oidcTokenHelper.parseJWT(revocationRequest.getToken());
				tokenIdToRevoke = token.getBody().get(OIDCClaimName.refresh_token_id.name(), String.class);
				if (tokenIdToRevoke == null) {
					throw new IllegalArgumentException("The access token was not issued via a refresh token. It cannot be revoked.");
				}
				// Don't check if the client owns the refresh token
				// If someone other than the authorized client has an unexpired, signed access token, it should be revoked anyways
				break;
			case refresh_token: // retrieve the token ID from the DAO using the token hash
				String hashedToken = hashToken(revocationRequest.getToken());
				OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getMatchingTokenByHashForUpdate(hashedToken, clientId)
						.orElseThrow(() -> new NotFoundException("The refresh token does not exist."));
				tokenIdToRevoke= metadata.getTokenId();
				break;
			default:
				throw new IllegalArgumentException("Unable to revoke a token with token_type_hint=" + revocationRequest.getToken_type_hint().name());
		}

		oauthRefreshTokenDao.deleteToken(tokenIdToRevoke);
	}

	@MandatoryWriteTransaction
	@Override
	public String rotateRefreshToken(String tokenId) {
		// This method should only be called when the token row is locked, so something is wrong on our end if the token doesn't exist anymore
		OAuthRefreshTokenInformation metadata = oauthRefreshTokenDao.getRefreshTokenMetadata(tokenId)
				.orElseThrow(() -> new IllegalStateException("Attempted to rotate a refresh token that does not exist."));

		String token = generateRefreshToken();
		String hash = hashToken(token);

		// Update the etag, and last used date (not modified on, which tells when the name was last changed)
		metadata.setLastUsed(clock.now());
		metadata.setEtag(UUID.randomUUID().toString());

		oauthRefreshTokenDao.updateTokenHash(metadata, hash);
		return token;
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
